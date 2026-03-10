package com.finventory.dto;

import com.finventory.model.MigrationRunStatus;
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
public class MigrationRunDto {
    private UUID id;
    private String sourceSystem;
    private String sourceReference;
    private boolean dryRun;
    private MigrationRunStatus status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String requestedBy;
    private Long scopeSourceIdMin;
    private Long scopeSourceIdMax;
    private Integer scopeLimit;
}
