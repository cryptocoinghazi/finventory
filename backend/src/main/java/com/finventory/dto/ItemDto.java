package com.finventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemDto {
    private UUID id;

    @NotBlank(message = "Item name is required")
    private String name;

    @NotBlank(message = "Item code is required")
    private String code;

    private String hsnCode;

    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Tax rate must be positive or zero")
    private BigDecimal taxRate;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be positive or zero")
    private BigDecimal unitPrice;

    @NotBlank(message = "Unit of measurement (UOM) is required")
    private String uom;

    private UUID vendorId;
    private String vendorName;
}
