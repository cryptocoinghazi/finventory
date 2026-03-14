package com.finventory.dto;

import com.finventory.model.OfferDiscountType;
import com.finventory.model.OfferScope;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferValidationResponse {

    private UUID offerId;
    private String code;
    private String name;
    private OfferScope scope;
    private OfferDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
}
