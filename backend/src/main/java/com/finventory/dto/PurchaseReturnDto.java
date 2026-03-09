package com.finventory.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReturnDto {

    private UUID id;

    private String returnNumber;

    private UUID purchaseInvoiceId;

    @NotNull(message = "Return date is required")
    private LocalDate returnDate;

    @NotNull(message = "Party ID is required")
    private UUID partyId;
    private String partyName;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;
    private String warehouseName;

    private BigDecimal totalTaxableAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalCgstAmount;
    private BigDecimal totalSgstAmount;
    private BigDecimal totalIgstAmount;
    private BigDecimal grandTotal;

    @NotNull(message = "Lines are required")
    private List<PurchaseReturnLineDto> lines;
}
