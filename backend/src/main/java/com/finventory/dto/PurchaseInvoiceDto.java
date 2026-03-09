package com.finventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class PurchaseInvoiceDto {

    private UUID id;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotNull(message = "Party ID is required")
    private UUID partyId;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private String partyName;
    private String warehouseName;

    private String invoiceNumber;

    private String vendorInvoiceNumber;

    @NotEmpty(message = "Invoice must have at least one line item")
    @Valid
    private List<PurchaseInvoiceLineDto> lines;

    // Calculated fields
    private BigDecimal totalTaxableAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalCgstAmount;
    private BigDecimal totalSgstAmount;
    private BigDecimal totalIgstAmount;
    private BigDecimal grandTotal;
}
