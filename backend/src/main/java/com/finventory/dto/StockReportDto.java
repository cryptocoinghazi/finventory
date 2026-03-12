package com.finventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class StockReportDto {

    @Data
    @Builder
    public static class Row {
        private UUID itemId;
        private String itemName;
        private String itemCode;
        private UUID vendorId;
        private String vendorName;
        private UUID warehouseId;
        private String warehouseName;
        private BigDecimal currentStock;
        private String uom;
        private BigDecimal unitPrice;
        private BigDecimal valuation;
    }

    @Data
    @Builder
    public static class LowStockResponse {
        private BigDecimal threshold;
        private List<Row> lowStock;
        private List<Row> outOfStock;
    }

    @Data
    @Builder
    public static class MovementEntry {
        private LocalDate date;
        private UUID itemId;
        private String itemName;
        private UUID warehouseId;
        private String warehouseName;
        private BigDecimal qtyIn;
        private BigDecimal qtyOut;
        private String refType;
        private UUID refId;
    }
}
