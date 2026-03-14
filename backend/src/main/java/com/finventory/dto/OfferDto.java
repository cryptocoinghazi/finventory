package com.finventory.dto;

import com.finventory.model.OfferDiscountType;
import com.finventory.model.OfferScope;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class OfferDto {

    private UUID id;

    @NotBlank(message = "Name is required")
    private String name;

    private String code;

    @NotNull(message = "Discount type is required")
    private OfferDiscountType discountType;

    @NotNull(message = "Scope is required")
    private OfferScope scope;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    private UUID itemId;

    private LocalDate startDate;
    private LocalDate endDate;

    private Boolean active;

    private Integer usageLimit;
    private Integer usedCount;

    private BigDecimal minBillAmount;
}
