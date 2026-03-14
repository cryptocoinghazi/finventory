package com.finventory.dto;

import com.finventory.model.MigrationLogLevel;
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
public class MigrationLogEntryDto {
    private UUID id;
    private UUID runId;
    private String stageKey;
    private MigrationLogLevel level;
    private String message;
    private String details;
    private OffsetDateTime createdAt;
}
