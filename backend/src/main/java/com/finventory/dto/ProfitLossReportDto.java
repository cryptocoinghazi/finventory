package com.finventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfitLossReportDto {
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal revenue;
    private BigDecimal discounts;
    private BigDecimal expenses;
    private BigDecimal cogs;
    private BigDecimal grossProfit;
    private BigDecimal netProfit;
}
