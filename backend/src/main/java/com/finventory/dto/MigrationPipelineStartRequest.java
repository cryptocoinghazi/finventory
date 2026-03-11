package com.finventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationPipelineStartRequest {
    private MigrationPipelinePreset preset;
    private Boolean confirmed;
}
