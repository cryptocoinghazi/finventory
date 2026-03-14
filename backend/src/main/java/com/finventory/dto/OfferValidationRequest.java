package com.finventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class OfferValidationRequest {

    @NotBlank(message = "Code is required")
    private String code;

    private LocalDate asOfDate;

    @NotNull(message = "Taxable subtotal is required")
    private BigDecimal taxableSubtotal;

    @NotEmpty(message = "At least one line is required")
    @Valid
    private List<OfferValidationLine> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferValidationLine {
        @NotNull(message = "Item ID is required")
        private UUID itemId;

        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;

        @NotNull(message = "Unit price is required")
        private BigDecimal unitPrice;
    }
}
