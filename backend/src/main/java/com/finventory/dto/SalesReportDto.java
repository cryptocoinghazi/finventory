package com.finventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

public class SalesReportDto {

    @Data
    @Builder
    public static class Response {
        private LocalDate fromDate;
        private LocalDate toDate;
        private String groupBy;
        private Totals totals;
        private List<Bucket> buckets;
    }

    @Data
    @Builder
    public static class Totals {
        private long invoiceCount;
        private BigDecimal totalAmount;
        private BigDecimal totalDiscount;
        private BigDecimal totalPaid;
        private BigDecimal totalPending;
    }

    @Data
    @Builder
    public static class Bucket {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String label;
        private long invoiceCount;
        private BigDecimal totalAmount;
        private BigDecimal totalDiscount;
        private BigDecimal totalPaid;
        private BigDecimal totalPending;
    }
}
