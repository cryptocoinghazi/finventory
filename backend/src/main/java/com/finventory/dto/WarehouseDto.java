package com.finventory.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WarehouseDto {
    private UUID id;

    @NotBlank(message = "Warehouse name is required")
    private String name;

    private String stateCode;

    private String location;
}
