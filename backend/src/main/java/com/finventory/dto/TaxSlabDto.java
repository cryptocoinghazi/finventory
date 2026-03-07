package com.finventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaxSlabDto {
  private UUID id;

  @NotNull(message = "Rate is required")
  @DecimalMin(value = "0.0", inclusive = true, message = "Rate must be positive or zero")
  private BigDecimal rate;

  private String description;
}
