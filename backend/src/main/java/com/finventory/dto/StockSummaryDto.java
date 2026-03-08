package com.finventory.dto;

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
public class StockSummaryDto {
  private UUID itemId;
  private String itemName;
  private String itemCode;
  private UUID warehouseId;
  private String warehouseName;
  private BigDecimal currentStock;
  private String uom;
}
