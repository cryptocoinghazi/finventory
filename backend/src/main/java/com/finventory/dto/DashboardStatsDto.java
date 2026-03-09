package com.finventory.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDto {
    private BigDecimal salesToday;
    private BigDecimal purchaseToday;
    private BigDecimal stockValue;
    private BigDecimal outstanding;
}
