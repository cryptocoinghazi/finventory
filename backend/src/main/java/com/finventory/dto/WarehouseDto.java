package com.finventory.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WarehouseDto {
  private UUID id;
  private String name;
  private String location;
}
