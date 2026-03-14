package com.finventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMigrationRunRequest {
    private String sourceSystem;
    private String sourceReference;
    private Boolean dryRun;
    private Long sourceIdMin;
    private Long sourceIdMax;
    private Integer limit;
}
