package com.finventory.dto;

import com.finventory.model.MigrationStageStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationStageExecutionDto {
    private UUID id;
    private UUID runId;
    private String stageKey;
    private MigrationStageStatus status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String statsJson;
    private String errorMessage;
}
