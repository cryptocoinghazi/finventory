package com.finventory.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentDto {

    private UUID id;

    private String adjustmentNumber;

    @NotNull(message = "Adjustment Date is required")
    private LocalDate adjustmentDate;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private String warehouseName;

    @NotNull(message = "Item ID is required")
    private UUID itemId;

    private String itemName;

    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;

    private String reason;
}
