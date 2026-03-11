package com.finventory.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LabelPrintInvalidItemDto {
    private UUID itemId;
    private String name;
    private String code;
    private String barcode;
    private BigDecimal unitPrice;
    private Integer quantity;
    private List<String> errors;
}

