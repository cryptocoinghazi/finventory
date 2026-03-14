package com.finventory.dto;

import com.finventory.model.DatabaseBackupStatus;
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
public class DatabaseBackupDto {
    private UUID id;
    private DatabaseBackupStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String requestedBy;
    private String fileName;
    private Long fileSize;
    private String errorMessage;
}
