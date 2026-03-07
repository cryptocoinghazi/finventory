package com.finventory.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaxSlabDto {
  private UUID id;
  private BigDecimal rate;
  private String description;
}
