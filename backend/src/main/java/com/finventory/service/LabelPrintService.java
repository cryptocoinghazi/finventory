package com.finventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.LabelPrintInvalidItemDto;
import com.finventory.dto.LabelPrintPrepareLineRequest;
import com.finventory.dto.LabelPrintPrepareRequest;
import com.finventory.dto.LabelPrintPrepareResponse;
import com.finventory.dto.LabelPrintPreparedItemDto;
import com.finventory.model.Item;
import com.finventory.model.LabelBarcodeFormat;
import com.finventory.model.LabelPrintJob;
import com.finventory.model.LabelPrintJobStatus;
import com.finventory.model.User;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.LabelPrintJobRepository;
import com.finventory.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabelPrintService {
    private static final int EAN13_LENGTH = 13;
    private static final int EAN13_DATA_LENGTH = 12;
    private static final int EAN13_WEIGHT_EVEN_POSITION = 3;
    private static final int CHECKSUM_MOD = 10;

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final LabelPrintJobRepository labelPrintJobRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public LabelPrintPrepareResponse prepare(LabelPrintPrepareRequest request, String requestedBy) {
        User user =
                userRepository
                        .findByUsername(requestedBy)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean includeItemCode =
                request.getIncludeItemCode() != null && request.getIncludeItemCode();

        Map<UUID, Integer> quantityByItemId = mergeRequestedQuantities(request.getItems());
        List<UUID> requestedIds = new ArrayList<>(quantityByItemId.keySet());
        List<Item> items = itemRepository.findAllById(requestedIds);
        Map<UUID, Item> itemById = new LinkedHashMap<>();
        for (Item it : items) {
            itemById.put(it.getId(), it);
        }

        LinkedHashSet<UUID> missingIds = new LinkedHashSet<>();
        for (UUID id : requestedIds) {
            if (!itemById.containsKey(id)) {
                missingIds.add(id);
            }
        }

        List<LabelPrintPreparedItemDto> validItems = new ArrayList<>();
        List<LabelPrintInvalidItemDto> invalidItems = new ArrayList<>();

        for (UUID itemId : requestedIds) {
            Integer qty = quantityByItemId.getOrDefault(itemId, 0);
            if (missingIds.contains(itemId)) {
                invalidItems.add(
                        LabelPrintInvalidItemDto.builder()
                                .itemId(itemId)
                                .quantity(qty)
                                .errors(List.of("Item not found"))
                                .build());
                continue;
            }
            Item item = itemById.get(itemId);
            List<String> errors = validateItem(item, request.getBarcodeFormat());
            if (!errors.isEmpty()) {
                invalidItems.add(
                        LabelPrintInvalidItemDto.builder()
                                .itemId(item.getId())
                                .name(item.getName())
                                .code(item.getCode())
                                .barcode(item.getBarcode())
                                .unitPrice(item.getUnitPrice())
                                .quantity(qty)
                                .errors(errors)
                                .build());
                continue;
            }

            LabelBarcodeFormat effective = resolveEffectiveBarcodeFormat(item.getBarcode(), request.getBarcodeFormat());
            validItems.add(
                    LabelPrintPreparedItemDto.builder()
                            .itemId(item.getId())
                            .name(item.getName())
                            .code(item.getCode())
                            .barcode(item.getBarcode())
                            .unitPrice(item.getUnitPrice())
                            .quantity(qty)
                            .effectiveBarcodeFormat(effective)
                            .build());
        }

        int totalLabelsRequested = quantityByItemId.values().stream().mapToInt(Integer::intValue).sum();
        int totalLabelsValid = validItems.stream().mapToInt(LabelPrintPreparedItemDto::getQuantity).sum();

        LabelPrintJobStatus status =
                invalidItems.isEmpty() ? LabelPrintJobStatus.PREPARED : LabelPrintJobStatus.FAILED_VALIDATION;

        String detailsJson = buildDetailsJson(request, includeItemCode, validItems, invalidItems);

        LabelPrintJob job =
                LabelPrintJob.builder()
                        .user(user)
                        .status(status)
                        .templateName(request.getTemplateName())
                        .barcodeFormat(request.getBarcodeFormat())
                        .totalLabels(totalLabelsRequested)
                        .detailsJson(detailsJson)
                        .build();

        LabelPrintJob saved = labelPrintJobRepository.save(job);

        return LabelPrintPrepareResponse.builder()
                .jobId(saved.getId())
                .status(saved.getStatus())
                .templateName(saved.getTemplateName())
                .requestedBarcodeFormat(saved.getBarcodeFormat())
                .includeItemCode(includeItemCode)
                .totalLabelsRequested(totalLabelsRequested)
                .totalLabelsValid(totalLabelsValid)
                .items(validItems)
                .invalidItems(invalidItems)
                .build();
    }

    private Map<UUID, Integer> mergeRequestedQuantities(List<LabelPrintPrepareLineRequest> items) {
        Map<UUID, Integer> quantityByItemId = new LinkedHashMap<>();
        for (LabelPrintPrepareLineRequest line : items) {
            UUID itemId = line.getItemId();
            Integer qty = line.getQuantity() == null ? 0 : line.getQuantity();
            quantityByItemId.merge(itemId, qty, Integer::sum);
        }
        return quantityByItemId;
    }

    private List<String> validateItem(Item item, LabelBarcodeFormat requestedFormat) {
        List<String> errors = new ArrayList<>();

        if (item.getBarcode() == null || item.getBarcode().isBlank()) {
            errors.add("Missing barcode");
        } else {
            String barcode = item.getBarcode().trim();
            if (requestedFormat == LabelBarcodeFormat.EAN13
                    || requestedFormat == LabelBarcodeFormat.AUTO) {
                if (requestedFormat == LabelBarcodeFormat.EAN13 && !isValidEan13(barcode)) {
                    errors.add("Barcode is not a valid EAN-13 value");
                }
            }
        }

        if (item.getUnitPrice() == null) {
            errors.add("Missing selling price");
        }

        return errors;
    }

    private LabelBarcodeFormat resolveEffectiveBarcodeFormat(
            String barcode, LabelBarcodeFormat requestedFormat) {
        if (requestedFormat == LabelBarcodeFormat.AUTO) {
            return isValidEan13(barcode) ? LabelBarcodeFormat.EAN13 : LabelBarcodeFormat.CODE128;
        }
        return requestedFormat;
    }

    private boolean isValidEan13(String barcode) {
        if (barcode == null) {
            return false;
        }
        String v = barcode.trim();
        if (v.length() != EAN13_LENGTH) {
            return false;
        }
        for (int i = 0; i < v.length(); i++) {
            if (!Character.isDigit(v.charAt(i))) {
                return false;
            }
        }
        int sum = 0;
        for (int i = 0; i < EAN13_DATA_LENGTH; i++) {
            int digit = v.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * EAN13_WEIGHT_EVEN_POSITION;
        }
        int check = (CHECKSUM_MOD - (sum % CHECKSUM_MOD)) % CHECKSUM_MOD;
        return check == (v.charAt(EAN13_DATA_LENGTH) - '0');
    }

    private String buildDetailsJson(
            LabelPrintPrepareRequest request,
            boolean includeItemCode,
            List<LabelPrintPreparedItemDto> validItems,
            List<LabelPrintInvalidItemDto> invalidItems) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("templateName", request.getTemplateName().name());
        details.put("barcodeFormat", request.getBarcodeFormat().name());
        details.put("includeItemCode", includeItemCode);
        details.put("items", validItems);
        details.put("invalidItems", invalidItems);
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize label print details");
        }
    }
}
