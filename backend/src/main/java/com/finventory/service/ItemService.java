package com.finventory.service;

import com.finventory.dto.ItemDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ItemService {
    private static final int BARCODE_GENERATE_MAX_ATTEMPTS = 10;
    private static final int BARCODE_PREFIX_MAX_LENGTH = 20;
    private static final int SAFE_EXTENSION_MAX_LENGTH = 10;
    private static final int BARCODE_RANDOM_HEX_LENGTH = 12;
    private static final int ITEM_CODE_MAX_LENGTH = 64;
    private static final int ITEM_CODE_RANDOM_LENGTH = 6;
    private static final int ITEM_CODE_GENERATE_MAX_ATTEMPTS = 20;
    private static final int IMAGE_MAX_BYTES = 25 * 1024 * 1024;
    private static final int IMAGE_RESIZE_MAX_DIMENSION = 1024;
    private static final float JPEG_QUALITY = 0.85f;
    private static final Duration REMOTE_IMAGE_TIMEOUT = Duration.ofSeconds(12);
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX_EXCLUSIVE = 300;
    private static final int IO_BUFFER_SIZE = 8192;
    private static final int MIN_IMAGE_MAGIC_BYTES = 12;
    private static final int WEBP_MAGIC_OFFSET = 8;
    private static final byte[] JPEG_MAGIC = new byte[] {(byte) 0xFF, (byte) 0xD8};
    private static final byte[] PNG_MAGIC = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] RIFF_MAGIC = new byte[] {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = new byte[] {0x57, 0x45, 0x42, 0x50};

    private static final List<String> BULK_CSV_HEADERS =
            List.of("Name", "Preferred Vendor", "UOM", "COGS", "UNIT price", "Image path or URL");
    private static final String DEFAULT_UOM = "pcs";

    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;
    private final AuditLogService auditLogService;
    private final PlatformTransactionManager transactionManager;
    private final Validator validator;

    @Value("${application.uploads.dir:uploads}")
    private String uploadsDir;

    public BulkItemUploadResponse uploadItemsReport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        HttpClient httpClient = buildHttpClient();

        try (CSVReader csv = buildCsvReader(file)) {
            BulkUploadAccumulator acc = processBulkCsv(csv, txTemplate, httpClient);
            return new BulkItemUploadResponse(
                    acc.totalRows,
                    acc.createdCount,
                    acc.skippedCount,
                    acc.failedCount,
                    acc.issues,
                    acc.missingImages);
        } catch (CsvValidationException ex) {
            throw new IllegalArgumentException("Invalid CSV file: " + ex.getMessage(), ex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage(), e);
        }
    }

    public List<ItemDto> uploadItemsLegacy(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        HttpClient httpClient = buildHttpClient();

        try (CSVReader csv = buildCsvReader(file)) {
            Map<String, Integer> indexByHeader = parseAndValidateBulkHeaders(csv);

            List<ItemDto> createdItems = new ArrayList<>();
            String[] row;
            int rowNumber = 1;
            while ((row = csv.readNext()) != null) {
                rowNumber++;
                BulkRowOutcome outcome =
                        processBulkRow(rowNumber, row, indexByHeader, txTemplate, httpClient);
                if (outcome.createdItem != null) {
                    createdItems.add(outcome.createdItem);
                }
            }
            return createdItems;
        } catch (CsvValidationException ex) {
            throw new IllegalArgumentException("Invalid CSV file: " + ex.getMessage(), ex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process CSV file: " + e.getMessage(), e);
        }
    }

    private HttpClient buildHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(REMOTE_IMAGE_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private CSVReader buildCsvReader(MultipartFile file) throws Exception {
        return new CSVReaderBuilder(
                        new BufferedReader(
                                new InputStreamReader(
                                        file.getInputStream(), StandardCharsets.UTF_8)))
                .withCSVParser(
                        new CSVParserBuilder()
                                .withSeparator(',')
                                .withIgnoreLeadingWhiteSpace(false)
                                .withEscapeChar(CSVParser.NULL_CHARACTER)
                                .build())
                .build();
    }

    private BulkUploadAccumulator processBulkCsv(
            CSVReader csv, TransactionTemplate txTemplate, HttpClient httpClient) throws Exception {
        Map<String, Integer> indexByHeader = parseAndValidateBulkHeaders(csv);

        int totalRows = 0;
        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<BulkItemUploadRowResult> issues = new ArrayList<>();
        List<BulkItemCreatedWithoutImage> missingImages = new ArrayList<>();

        String[] row;
        int rowNumber = 1;
        while ((row = csv.readNext()) != null) {
            rowNumber++;
            totalRows++;

            BulkRowOutcome outcome =
                    processBulkRow(rowNumber, row, indexByHeader, txTemplate, httpClient);
            if (outcome.status == BulkItemUploadRowStatus.CREATED) {
                created++;
                if (outcome.missingImage && outcome.createdItem != null) {
                    missingImages.add(toMissingImage(outcome.createdItem));
                }
                continue;
            }
            if (outcome.status == BulkItemUploadRowStatus.SKIPPED) {
                skipped++;
                issues.add(outcome.result);
                continue;
            }
            if (outcome.status == BulkItemUploadRowStatus.FAILED) {
                failed++;
                issues.add(outcome.result);
                continue;
            }
            if (outcome.status == BulkItemUploadRowStatus.CREATED_WITH_WARNING) {
                created++;
                issues.add(outcome.result);
                if (outcome.createdItem != null) {
                    missingImages.add(toMissingImage(outcome.createdItem));
                }
            }
        }

        return new BulkUploadAccumulator(
                totalRows, created, skipped, failed, issues, missingImages);
    }

    private BulkItemCreatedWithoutImage toMissingImage(ItemDto item) {
        return new BulkItemCreatedWithoutImage(
                item.getId(), item.getCode(), item.getName(), item.getVendorName());
    }

    private Map<String, Integer> parseAndValidateBulkHeaders(CSVReader csv) throws Exception {
        String[] rawHeader = csv.readNext();
        if (rawHeader == null) {
            throw new IllegalArgumentException("CSV is empty");
        }

        List<String> header =
                Arrays.stream(rawHeader).map(this::normalizeCsvHeader).collect(Collectors.toList());
        LinkedHashSet<String> expected = new LinkedHashSet<>(BULK_CSV_HEADERS);
        LinkedHashSet<String> actual = new LinkedHashSet<>(header);
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException(
                    "Invalid CSV headers. Expected: " + String.join(", ", BULK_CSV_HEADERS));
        }

        Map<String, Integer> indexByHeader = new LinkedHashMap<>();
        for (int i = 0; i < header.size(); i++) {
            indexByHeader.put(header.get(i), i);
        }
        return indexByHeader;
    }

    private BulkRowOutcome processBulkRow(
            int rowNumber,
            String[] row,
            Map<String, Integer> indexByHeader,
            TransactionTemplate txTemplate,
            HttpClient httpClient) {
        String name = normalizeNullable(cell(row, indexByHeader.get("Name")));
        String vendorName = normalizeNullable(cell(row, indexByHeader.get("Preferred Vendor")));
        String uom = normalizeNullable(cell(row, indexByHeader.get("UOM")));
        String cogsRaw = normalizeNullable(cell(row, indexByHeader.get("COGS")));
        String unitPriceRaw = normalizeNullable(cell(row, indexByHeader.get("UNIT price")));
        String imageSource =
                normalizeImageSource(cell(row, indexByHeader.get("Image path or URL")));

        List<String> missing = new ArrayList<>();
        if (name == null) {
            missing.add("Name");
        }
        if (vendorName == null) {
            missing.add("Preferred Vendor");
        }
        if (cogsRaw == null) {
            missing.add("COGS");
        }
        if (unitPriceRaw == null) {
            missing.add("UNIT price");
        }
        if (!missing.isEmpty()) {
            return BulkRowOutcome.skipped(
                    rowNumber, "Missing mandatory fields: " + String.join(", ", missing));
        }

        Party vendor = resolveVendorByName(vendorName);
        if (vendor == null) {
            return BulkRowOutcome.skipped(rowNumber, "Vendor not found: " + vendorName);
        }

        BigDecimal cogs;
        BigDecimal unitPrice;
        try {
            cogs = new BigDecimal(cogsRaw);
            unitPrice = new BigDecimal(unitPriceRaw);
        } catch (Exception ex) {
            return BulkRowOutcome.skipped(
                    rowNumber, "Invalid numeric value for COGS or UNIT price");
        }

        if (cogs.compareTo(BigDecimal.ZERO) < 0 || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            return BulkRowOutcome.skipped(rowNumber, "COGS and UNIT price must be >= 0");
        }

        String finalUom = uom != null ? uom : DEFAULT_UOM;
        String code = generateUniqueItemCode(vendor.getName(), name);

        ItemDto dto =
                ItemDto.builder()
                        .name(name)
                        .code(code)
                        .barcode(null)
                        .hsnCode(null)
                        .category(null)
                        .taxRate(BigDecimal.ZERO)
                        .unitPrice(unitPrice)
                        .cogs(cogs)
                        .uom(finalUom)
                        .imageUrl(null)
                        .vendorId(vendor.getId())
                        .build();

        List<String> violations = validateItemDto(dto);
        if (!violations.isEmpty()) {
            return BulkRowOutcome.skipped(rowNumber, String.join("; ", violations));
        }

        try {
            ItemDto createdItem = inTx(txTemplate, () -> createItem(dto));
            if (createdItem == null) {
                return BulkRowOutcome.failed(rowNumber, "Failed to create item");
            }

            if (imageSource == null) {
                return BulkRowOutcome.created(createdItem, true);
            }

            String warning =
                    tryAttachImage(createdItem.getId(), imageSource, txTemplate, httpClient);
            if (warning == null) {
                return BulkRowOutcome.created(createdItem, false);
            }
            return BulkRowOutcome.createdWithWarning(
                    rowNumber, "Created item " + code, createdItem.getId(), warning, createdItem);
        } catch (Exception ex) {
            return BulkRowOutcome.failed(
                    rowNumber, ex.getMessage() != null ? ex.getMessage() : "Failed to create item");
        }
    }

    private String tryAttachImage(
            UUID itemId,
            String imageSource,
            TransactionTemplate txTemplate,
            HttpClient httpClient) {
        try {
            byte[] bytes = loadImageBytes(imageSource, httpClient);
            String ext = detectImageExtension(imageSource, bytes);
            if (ext == null) {
                return "Image skipped: Unsupported image format";
            }
            byte[] processed = processImageBytes(bytes, ext);
            inTx(
                    txTemplate,
                    () -> {
                        storeItemImage(itemId, processed, ext);
                        return null;
                    });
            return null;
        } catch (Exception ex) {
            return "Image skipped: "
                    + (ex.getMessage() != null ? ex.getMessage() : "invalid image");
        }
    }

    private String normalizeImageSource(String value) {
        String v = normalizeNullable(value);
        if (v == null) {
            return null;
        }
        String s = v.trim();
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                s = s.substring(1, s.length() - 1).trim();
            }
        }
        return s.isBlank() ? null : s;
    }

    private <T> T inTx(TransactionTemplate txTemplate, TxSupplier<T> supplier) {
        return txTemplate.execute(
                status -> {
                    try {
                        return supplier.get();
                    } catch (RuntimeException ex) {
                        status.setRollbackOnly();
                        throw ex;
                    }
                });
    }

    @FunctionalInterface
    private interface TxSupplier<T> {
        T get();
    }

    private record BulkUploadAccumulator(
            int totalRows,
            int createdCount,
            int skippedCount,
            int failedCount,
            List<BulkItemUploadRowResult> issues,
            List<BulkItemCreatedWithoutImage> missingImages) {}

    private record BulkRowOutcome(
            BulkItemUploadRowStatus status,
            BulkItemUploadRowResult result,
            @Nullable ItemDto createdItem,
            boolean missingImage) {
        static BulkRowOutcome created(ItemDto createdItem, boolean missingImage) {
            return new BulkRowOutcome(
                    BulkItemUploadRowStatus.CREATED, null, createdItem, missingImage);
        }

        static BulkRowOutcome skipped(int rowNumber, String message) {
            return new BulkRowOutcome(
                    BulkItemUploadRowStatus.SKIPPED,
                    new BulkItemUploadRowResult(
                            rowNumber, BulkItemUploadRowStatus.SKIPPED, message, null, null),
                    null,
                    false);
        }

        static BulkRowOutcome failed(int rowNumber, String message) {
            return new BulkRowOutcome(
                    BulkItemUploadRowStatus.FAILED,
                    new BulkItemUploadRowResult(
                            rowNumber, BulkItemUploadRowStatus.FAILED, message, null, null),
                    null,
                    false);
        }

        static BulkRowOutcome createdWithWarning(
                int rowNumber, String message, UUID itemId, String warning, ItemDto createdItem) {
            return new BulkRowOutcome(
                    BulkItemUploadRowStatus.CREATED_WITH_WARNING,
                    new BulkItemUploadRowResult(
                            rowNumber,
                            BulkItemUploadRowStatus.CREATED_WITH_WARNING,
                            message,
                            itemId,
                            warning),
                    createdItem,
                    true);
        }
    }

    public ItemDto createItem(ItemDto dto) {
        if (itemRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException(
                    "Item with code " + dto.getCode() + " already exists");
        }

        Party vendor;
        if (dto.getVendorId() != null) {
            vendor =
                    partyRepository
                            .findById(dto.getVendorId())
                            .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
            if (vendor.getType() != Party.PartyType.VENDOR) {
                throw new IllegalArgumentException("Selected party is not a vendor");
            }
        } else {
            vendor = getOrCreateDefaultVendor();
        }

        String normalizedBarcode = normalizeBarcode(dto.getBarcode());
        if (normalizedBarcode != null) {
            validateBarcodeAvailable(normalizedBarcode, null);
        } else {
            normalizedBarcode = generateUniqueBarcode(dto.getCode());
        }

        Item item =
                Item.builder()
                        .name(dto.getName())
                        .code(dto.getCode())
                        .barcode(normalizedBarcode)
                        .hsnCode(dto.getHsnCode())
                        .category(normalizeNullable(dto.getCategory()))
                        .taxRate(dto.getTaxRate())
                        .unitPrice(dto.getUnitPrice())
                        .cogs(dto.getCogs() != null ? dto.getCogs() : java.math.BigDecimal.ZERO)
                        .uom(dto.getUom())
                        .imageUrl(normalizeNullable(dto.getImageUrl()))
                        .vendor(vendor)
                        .build();

        Item saved = itemRepository.save(item);
        auditLogService.log(
                "ITEM_CREATED",
                "ITEM",
                saved.getId(),
                "code="
                        + saved.getCode()
                        + "; name="
                        + saved.getName()
                        + "; unitPrice="
                        + saved.getUnitPrice()
                        + "; uom="
                        + saved.getUom()
                        + "; barcode="
                        + saved.getBarcode());
        return mapToDto(saved);
    }

    public List<ItemDto> getAllItems() {
        return itemRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ItemDto getItemById(UUID id) {
        return itemRepository
                .findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));
    }

    public ItemDto updateItem(UUID id, ItemDto dto) {
        Item existing =
                itemRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Item not found with id: " + id));

        existing.setName(dto.getName());
        existing.setHsnCode(dto.getHsnCode());
        existing.setCategory(normalizeNullable(dto.getCategory()));
        existing.setTaxRate(dto.getTaxRate());
        existing.setUnitPrice(dto.getUnitPrice());
        if (dto.getCogs() != null) {
            existing.setCogs(dto.getCogs());
        }
        existing.setUom(dto.getUom());
        if (dto.getImageUrl() != null) {
            existing.setImageUrl(normalizeNullable(dto.getImageUrl()));
        }

        if (dto.getVendorId() != null) {
            Party vendor =
                    partyRepository
                            .findById(dto.getVendorId())
                            .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
            if (vendor.getType() != Party.PartyType.VENDOR) {
                throw new IllegalArgumentException("Selected party is not a vendor");
            }
            existing.setVendor(vendor);
        } else {
            existing.setVendor(getOrCreateDefaultVendor());
        }

        if (!existing.getCode().equals(dto.getCode())) {
            if (itemRepository.existsByCode(dto.getCode())) {
                throw new IllegalArgumentException(
                        "Item with code " + dto.getCode() + " already exists");
            }
            existing.setCode(dto.getCode());
        }

        if (dto.getBarcode() != null) {
            String normalizedBarcode = normalizeBarcode(dto.getBarcode());
            if (normalizedBarcode != null) {
                if (!normalizedBarcode.equals(existing.getBarcode())) {
                    validateBarcodeAvailable(normalizedBarcode, existing.getId());
                    existing.setBarcode(normalizedBarcode);
                }
            } else {
                existing.setBarcode(null);
            }
        }

        return mapToDto(itemRepository.save(existing));
    }

    @Transactional
    public ItemDto uploadItemImage(UUID id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are supported");
        }
        try {
            String ext = safeFileExtension(file.getOriginalFilename());
            if (ext.isBlank()) {
                throw new IllegalArgumentException("Unsupported image format");
            }
            ext = ext.substring(1).toLowerCase(Locale.ROOT);
            byte[] bytes = readAllBytesWithLimit(file.getInputStream(), IMAGE_MAX_BYTES);
            byte[] processed = processImageBytes(bytes, ext);
            storeItemImage(id, processed, ext);
            return mapToDto(
                    itemRepository
                            .findById(id)
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Item not found with id: " + id)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to store uploaded image: " + e.getMessage(), e);
        }
    }

    public void deleteItem(UUID id) {
        itemRepository.deleteById(id);
    }

    private Party getOrCreateDefaultVendor() {
        List<Party> matches =
                partyRepository.findByNameIgnoreCaseAndType("Unassigned", Party.PartyType.VENDOR);
        if (matches != null && !matches.isEmpty()) {
            return matches.get(0);
        }

        Party created =
                Party.builder().name("Unassigned").type(Party.PartyType.VENDOR).build();
        try {
            return partyRepository.save(created);
        } catch (Exception ignored) {
            List<Party> vendors = partyRepository.findByType(Party.PartyType.VENDOR);
            if (vendors != null && !vendors.isEmpty()) {
                return vendors.get(0);
            }
            Party fallback =
                    Party.builder()
                            .name("Unassigned")
                            .type(Party.PartyType.VENDOR)
                            .build();
            return partyRepository.save(fallback);
        }
    }

    private ItemDto mapToDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .code(item.getCode())
                .barcode(item.getBarcode())
                .hsnCode(item.getHsnCode())
                .category(item.getCategory())
                .taxRate(item.getTaxRate())
                .unitPrice(item.getUnitPrice())
                .cogs(item.getCogs())
                .uom(item.getUom())
                .imageUrl(item.getImageUrl())
                .vendorId(item.getVendor() != null ? item.getVendor().getId() : null)
                .vendorName(item.getVendor() != null ? item.getVendor().getName() : null)
                .build();
    }

    private String normalizeBarcode(String barcode) {
        String normalized = normalizeNullable(barcode);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeFileExtension(String originalFilename) {
        String name = normalizeNullable(originalFilename);
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        String ext = name.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
        if (ext.isEmpty()
                || ext.length() > SAFE_EXTENSION_MAX_LENGTH
                || !ext.matches("[a-z0-9]+")) {
            return "";
        }
        return "." + ext;
    }

    private void validateBarcodeAvailable(String barcode, UUID currentItemId) {
        if (barcode == null) {
            return;
        }
        Optional<Item> existing = itemRepository.findByBarcode(barcode);
        if (existing.isPresent()
                && (currentItemId == null || !existing.get().getId().equals(currentItemId))) {
            throw new IllegalArgumentException("Item with barcode " + barcode + " already exists");
        }
    }

    private String generateUniqueBarcode(String code) {
        String prefix = normalizeNullable(code);
        if (prefix == null) {
            prefix = "ITEM";
        }
        prefix = prefix.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (prefix.isBlank()) {
            prefix = "ITEM";
        }
        if (prefix.length() > BARCODE_PREFIX_MAX_LENGTH) {
            prefix = prefix.substring(0, BARCODE_PREFIX_MAX_LENGTH);
        }

        for (int attempt = 0; attempt < BARCODE_GENERATE_MAX_ATTEMPTS; attempt++) {
            String suffix =
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, BARCODE_RANDOM_HEX_LENGTH)
                            .toUpperCase(Locale.ROOT);
            String candidate = prefix + "-" + suffix;
            if (!itemRepository.existsByBarcode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique barcode");
    }

    private String cell(String[] row, Integer idx) {
        if (idx == null || idx < 0 || idx >= row.length) {
            return null;
        }
        return row[idx];
    }

    private String normalizeCsvHeader(String header) {
        if (header == null) {
            return "";
        }
        String h = header;
        if (!h.isEmpty() && h.charAt(0) == '\uFEFF') {
            h = h.substring(1);
        }
        return h.trim();
    }

    private Party resolveVendorByName(String vendorName) {
        if (vendorName == null) {
            return null;
        }
        List<Party> matches =
                partyRepository.findByNameIgnoreCaseAndType(vendorName, Party.PartyType.VENDOR);
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        return matches.get(0);
    }

    private List<String> validateItemDto(ItemDto dto) {
        if (dto == null) {
            return List.of("Invalid item data");
        }
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .toList();
    }

    private String generateUniqueItemCode(String vendorName, String itemName) {
        String base =
                (normalizeNullable(vendorName) == null ? "" : normalizeNullable(vendorName))
                        + "--"
                        + (normalizeNullable(itemName) == null ? "" : normalizeNullable(itemName));
        base = slugify(base);
        if (base.isBlank()) {
            base = "item";
        }

        for (int attempt = 0; attempt < ITEM_CODE_GENERATE_MAX_ATTEMPTS; attempt++) {
            String suffix =
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, ITEM_CODE_RANDOM_LENGTH)
                            .toLowerCase(Locale.ROOT);
            String prefix = base;
            int maxPrefix = Math.max(1, ITEM_CODE_MAX_LENGTH - (suffix.length() + 1));
            if (prefix.length() > maxPrefix) {
                prefix = prefix.substring(0, maxPrefix);
            }
            String candidate = prefix + "-" + suffix;
            if (!itemRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique item code");
    }

    private String slugify(String input) {
        if (input == null) {
            return "";
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("(^-+)|(-+$)", "");
        return s;
    }

    private byte[] loadImageBytes(String source, HttpClient httpClient) throws Exception {
        if (source == null) {
            throw new IllegalArgumentException("Image source is empty");
        }

        String s = source.trim();
        if (s.startsWith("http://") || s.startsWith("https://")) {
            String resolved = normalizeDriveUrl(s);
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(URI.create(resolved))
                            .timeout(REMOTE_IMAGE_TIMEOUT)
                            .GET()
                            .build();
            HttpResponse<InputStream> res =
                    httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() < HTTP_SUCCESS_MIN
                    || res.statusCode() >= HTTP_SUCCESS_MAX_EXCLUSIVE) {
                throw new IllegalArgumentException("HTTP " + res.statusCode());
            }
            return readAllBytesWithLimit(res.body(), IMAGE_MAX_BYTES);
        }

        Path path;
        if (s.startsWith("file:")) {
            path = Path.of(URI.create(s));
        } else {
            path = Path.of(s);
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Local image path not found");
        }
        if (Files.size(path) > IMAGE_MAX_BYTES) {
            throw new IllegalArgumentException("Image too large");
        }
        return Files.readAllBytes(path);
    }

    private String normalizeDriveUrl(String url) {
        if (url == null) {
            return null;
        }
        if (!url.contains("drive.google.com")) {
            return url;
        }
        try {
            URI uri = URI.create(url);
            String path = uri.getPath() != null ? uri.getPath() : "";
            if (path.contains("/file/d/")) {
                String[] parts = path.split("/file/d/");
                if (parts.length > 1) {
                    String rest = parts[1];
                    String id = rest.split("/")[0];
                    if (!id.isBlank()) {
                        return "https://drive.google.com/uc?export=download&id=" + id;
                    }
                }
            }
            String query = uri.getQuery();
            if (query != null && query.contains("id=")) {
                for (String p : query.split("&")) {
                    if (p.startsWith("id=")) {
                        String id =
                                URLDecoder.decode(
                                        p.substring("id=".length()), StandardCharsets.UTF_8);
                        if (!id.isBlank()) {
                            return "https://drive.google.com/uc?export=download&id=" + id;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            return url;
        }
        return url;
    }

    private byte[] readAllBytesWithLimit(InputStream in, int maxBytes) throws Exception {
        if (in == null) {
            return new byte[0];
        }
        try (InputStream input = in;
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[IO_BUFFER_SIZE];
            int read;
            int total = 0;
            while ((read = input.read(buf)) >= 0) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("Image too large");
                }
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        }
    }

    private String detectImageExtension(String source, byte[] bytes) {
        String ext = null;
        if (source != null) {
            String s = source.trim().toLowerCase(Locale.ROOT);
            int q = s.indexOf('?');
            if (q >= 0) {
                s = s.substring(0, q);
            }
            int dot = s.lastIndexOf('.');
            if (dot >= 0 && dot < s.length() - 1) {
                String candidate = s.substring(dot + 1).trim();
                if (List.of("jpg", "jpeg", "png", "webp").contains(candidate)) {
                    ext = candidate;
                }
            }
        }
        if (ext != null) {
            return ext;
        }
        if (bytes != null && bytes.length >= MIN_IMAGE_MAGIC_BYTES) {
            if (startsWith(bytes, JPEG_MAGIC)) {
                return "jpg";
            }
            if (startsWith(bytes, PNG_MAGIC)) {
                return "png";
            }
            if (startsWith(bytes, RIFF_MAGIC) && matchesAt(bytes, WEBP_MAGIC_OFFSET, WEBP_MAGIC)) {
                return "webp";
            }
        }
        return null;
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes == null || prefix == null || bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAt(byte[] bytes, int offset, byte[] token) {
        if (bytes == null || token == null || offset < 0 || bytes.length < offset + token.length) {
            return false;
        }
        for (int i = 0; i < token.length; i++) {
            if (bytes[offset + i] != token[i]) {
                return false;
            }
        }
        return true;
    }

    private byte[] processImageBytes(byte[] bytes, String ext) throws Exception {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Empty image");
        }
        String e = ext.toLowerCase(Locale.ROOT);
        if (e.equals("webp")) {
            return bytes;
        }

        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (img == null) {
            throw new IllegalArgumentException("Invalid image");
        }
        BufferedImage resized = resizeIfNeeded(img, IMAGE_RESIZE_MAX_DIMENSION);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (e.equals("jpg") || e.equals("jpeg")) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(resized, null, null), param);
            } finally {
                writer.dispose();
            }
            return out.toByteArray();
        }

        ImageIO.write(resized, "png", out);
        return out.toByteArray();
    }

    private BufferedImage resizeIfNeeded(BufferedImage img, int maxDim) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w <= maxDim && h <= maxDim) {
            return img;
        }
        double scale = Math.min((double) maxDim / (double) w, (double) maxDim / (double) h);
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(img, 0, 0, nw, nh, null);
        g.dispose();
        return scaled;
    }

    private void storeItemImage(UUID itemId, byte[] bytes, String ext) {
        if (itemId == null) {
            throw new IllegalArgumentException("Item id is required");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Empty image");
        }

        String e = ext != null ? ext.toLowerCase(Locale.ROOT) : "";
        if (!List.of("jpg", "jpeg", "png", "webp").contains(e)) {
            throw new IllegalArgumentException("Unsupported image format");
        }

        Item item =
                itemRepository
                        .findById(itemId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Item not found with id: " + itemId));

        try {
            Path itemDir =
                    Path.of(uploadsDir)
                            .toAbsolutePath()
                            .normalize()
                            .resolve("items")
                            .resolve(itemId.toString());
            Files.createDirectories(itemDir);

            String filename = UUID.randomUUID().toString() + "." + e;
            Path target = itemDir.resolve(filename).normalize();
            Files.write(target, bytes);

            item.setImageUrl("/uploads/items/" + itemId + "/" + filename);
            itemRepository.save(item);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to store image: " + ex.getMessage(), ex);
        }
    }

    public record BulkItemUploadResponse(
            int totalRows,
            int createdCount,
            int skippedCount,
            int failedCount,
            List<BulkItemUploadRowResult> details,
            List<BulkItemCreatedWithoutImage> missingImages) {}

    public record BulkItemCreatedWithoutImage(
            UUID itemId, String code, String name, @Nullable String vendorName) {}

    public record BulkItemUploadRowResult(
            int rowNumber,
            BulkItemUploadRowStatus status,
            String message,
            @Nullable UUID itemId,
            @Nullable String warning) {}

    public enum BulkItemUploadRowStatus {
        CREATED,
        SKIPPED,
        FAILED,
        CREATED_WITH_WARNING
    }
}
