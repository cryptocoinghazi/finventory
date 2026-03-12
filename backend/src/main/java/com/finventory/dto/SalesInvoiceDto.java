package com.finventory.dto;

import com.finventory.model.InvoicePaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
public class SalesInvoiceDto {

    private UUID id;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotNull(message = "Party ID is required")
    private UUID partyId;

    @NotNull(message = "Warehouse ID is required")
    private UUID warehouseId;

    private String partyName;
    private String warehouseName;

    // Invoice number might be auto-generated or manual.
    // For now, let's assume manual input is allowed but uniqueness is checked.
    // Actually, usually it's auto-generated. I'll make it optional in input, but required in
    // output.
    private String invoiceNumber;

    @NotEmpty(message = "Invoice must have at least one line item")
    @Valid
    private List<SalesInvoiceLineDto> lines;

    // Calculated fields (read-only for request usually, but good to return in response)
    private BigDecimal totalTaxableAmount;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalCgstAmount;
    private BigDecimal totalSgstAmount;
    private BigDecimal totalIgstAmount;
    private BigDecimal grandTotal;

    private UUID offerId;
    private String offerCode;
    private BigDecimal offerDiscountAmount;

    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;

    private InvoicePaymentStatus paymentStatus;

    private OffsetDateTime cancelledAt;
    private OffsetDateTime deletedAt;
    private String cancelReason;
}
