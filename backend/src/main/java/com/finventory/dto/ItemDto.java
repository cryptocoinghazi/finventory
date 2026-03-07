package com.finventory.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ItemDto {
  private UUID id;
  private String name;
  private String code;
  private String hsnCode;
  private BigDecimal taxRate;
  private BigDecimal unitPrice;
  private String uom;
}
