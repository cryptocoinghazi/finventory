package com.finventory.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gstr3bDto {

    // Outward Supplies (3.1)
    private BigDecimal outwardTaxableValue;
    private BigDecimal outwardIgst;
    private BigDecimal outwardCgst;
    private BigDecimal outwardSgst;

    // ITC (4)
    private BigDecimal itcIgst;
    private BigDecimal itcCgst;
    private BigDecimal itcSgst;

    // Net Tax Payable
    private BigDecimal netTaxPayable;
}
