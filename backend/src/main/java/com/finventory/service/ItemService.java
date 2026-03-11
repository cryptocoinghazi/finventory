package com.finventory.service;

import com.finventory.dto.ItemCsvDto;
import com.finventory.dto.ItemDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ItemService {
    private static final int BARCODE_GENERATE_MAX_ATTEMPTS = 10;
    private static final int BARCODE_PREFIX_MAX_LENGTH = 20;
    private static final int SAFE_EXTENSION_MAX_LENGTH = 10;
    private static final int BARCODE_RANDOM_HEX_LENGTH = 12;

    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;

    @Value("${application.uploads.dir:uploads}")
    private String uploadsDir;

    @Transactional
    public List<ItemDto> uploadItems(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<ItemCsvDto> csvToBean =
                    new CsvToBeanBuilder<ItemCsvDto>(reader)
                            .withType(ItemCsvDto.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .build();

            List<ItemCsvDto> csvDtos = csvToBean.parse();
            List<Item> itemsToSave = new ArrayList<>();

            // Find a default vendor to assign to items
            List<Party> vendors = partyRepository.findByType(Party.PartyType.VENDOR);
            Party defaultVendor;
            if (!vendors.isEmpty()) {
                defaultVendor = vendors.get(0);
            } else {
                defaultVendor = Party.builder()
                        .name("General Vendor")
                        .type(Party.PartyType.VENDOR)
                        .address("Default Address")
                        .build();
                partyRepository.save(defaultVendor);
            }

            for (ItemCsvDto csvDto : csvDtos) {
                Optional<Item> existing = itemRepository.findByCode(csvDto.getCode());
                if (existing.isPresent()) {
                    Item item = existing.get();
                    item.setName(csvDto.getName());
                    item.setHsnCode(csvDto.getHsnCode());
                    item.setTaxRate(csvDto.getTaxRate());
                    item.setUnitPrice(csvDto.getUnitPrice());
                    item.setUom(csvDto.getUom());
                    String normalizedImageUrl = normalizeNullable(csvDto.getImageUrl());
                    if (normalizedImageUrl != null) {
                        item.setImageUrl(normalizedImageUrl);
                    }
                    String normalizedBarcode = normalizeBarcode(csvDto.getBarcode());
                    if (normalizedBarcode != null) {
                        validateBarcodeAvailable(normalizedBarcode, item.getId());
                        item.setBarcode(normalizedBarcode);
                    } else if (item.getBarcode() == null || item.getBarcode().isBlank()) {
                        item.setBarcode(generateUniqueBarcode(item.getCode()));
                    }
                    if (item.getVendor() == null) {
                        item.setVendor(defaultVendor);
                    }
                    itemsToSave.add(item);
                } else {
                    String normalizedBarcode = normalizeBarcode(csvDto.getBarcode());
                    if (normalizedBarcode != null) {
                        validateBarcodeAvailable(normalizedBarcode, null);
                    } else {
                        normalizedBarcode = generateUniqueBarcode(csvDto.getCode());
                    }
                    Item item =
                            Item.builder()
                                    .vendor(defaultVendor)
                                    .name(csvDto.getName())
                                    .code(csvDto.getCode())
                                    .barcode(normalizedBarcode)
                                    .hsnCode(csvDto.getHsnCode())
                                    .taxRate(csvDto.getTaxRate())
                                    .unitPrice(csvDto.getUnitPrice())
                                    .uom(csvDto.getUom())
                                    .imageUrl(normalizeNullable(csvDto.getImageUrl()))
                                    .build();
                    itemsToSave.add(item);
                }
            }

            List<Item> savedItems = itemRepository.saveAll(itemsToSave);
            return savedItems.stream().map(this::mapToDto).collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    public ItemDto createItem(ItemDto dto) {
        if (itemRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException(
                    "Item with code " + dto.getCode() + " already exists");
        }

        Party vendor;
        if (dto.getVendorId() != null) {
            vendor = partyRepository.findById(dto.getVendorId())
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
                        .taxRate(dto.getTaxRate())
                        .unitPrice(dto.getUnitPrice())
                        .cogs(dto.getCogs() != null ? dto.getCogs() : java.math.BigDecimal.ZERO)
                        .uom(dto.getUom())
                        .imageUrl(normalizeNullable(dto.getImageUrl()))
                        .vendor(vendor)
                        .build();

        Item saved = itemRepository.save(item);
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
            Party vendor = partyRepository.findById(dto.getVendorId())
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

        Item item =
                itemRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));

        try {
            Path itemDir = Path.of(uploadsDir).toAbsolutePath().normalize().resolve("items").resolve(id.toString());
            Files.createDirectories(itemDir);

            String extension = safeFileExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString() + extension;
            Path target = itemDir.resolve(filename).normalize();

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            item.setImageUrl("/uploads/items/" + id + "/" + filename);
            return mapToDto(itemRepository.save(item));
        } catch (Exception e) {
            throw new RuntimeException("Failed to store uploaded image: " + e.getMessage(), e);
        }
    }

    public void deleteItem(UUID id) {
        itemRepository.deleteById(id);
    }

    private Party getOrCreateDefaultVendor() {
        List<Party> vendors = partyRepository.findByType(Party.PartyType.VENDOR);
        if (!vendors.isEmpty()) {
            return vendors.get(0);
        }
        Party defaultVendor = Party.builder()
                .name("General Vendor")
                .type(Party.PartyType.VENDOR)
                .address("Default Address")
                .build();
        return partyRepository.save(defaultVendor);
    }

    private ItemDto mapToDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .code(item.getCode())
                .barcode(item.getBarcode())
                .hsnCode(item.getHsnCode())
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
        if (ext.isEmpty() || ext.length() > SAFE_EXTENSION_MAX_LENGTH || !ext.matches("[a-z0-9]+")) {
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
}
