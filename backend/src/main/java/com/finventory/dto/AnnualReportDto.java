package com.finventory.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnnualReportDto {
    private int year;
    private List<MonthTrend> months;
    private List<TopItem> topItems;
    private List<TopParty> topCustomers;

    @Data
    @Builder
    public static class MonthTrend {
        private int month;
        private BigDecimal revenue;
        private BigDecimal discounts;
        private BigDecimal expenses;
        private BigDecimal cogs;
        private BigDecimal grossProfit;
        private BigDecimal netProfit;
    }

    @Data
    @Builder
    public static class TopItem {
        private UUID itemId;
        private String itemName;
        private String itemCode;
        private BigDecimal quantity;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class TopParty {
        private UUID partyId;
        private String partyName;
        private BigDecimal amount;
        private long invoiceCount;
    }
}
