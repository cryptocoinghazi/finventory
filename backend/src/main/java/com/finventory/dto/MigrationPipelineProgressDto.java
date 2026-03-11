package com.finventory.dto;

import com.finventory.model.MigrationRunStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationPipelineProgressDto {
    private UUID runId;
    private MigrationRunStatus runStatus;
    private MigrationPipelinePreset preset;
    private boolean active;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String currentStage;
    private List<String> plannedStages;
    private List<String> completedStages;
    private String failedStage;
    private long warningsCount;
    private long errorsCount;
    private String summary;
}

