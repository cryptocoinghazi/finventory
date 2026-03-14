package com.finventory.controller;

import com.finventory.dto.CreateMigrationRunRequest;
import com.finventory.dto.DatabaseBackupDto;
import com.finventory.dto.MigrationPipelinePreset;
import com.finventory.dto.MigrationPipelineProgressDto;
import com.finventory.dto.MigrationPipelineStartRequest;
import com.finventory.dto.MigrationRunDto;
import com.finventory.dto.MigrationStageExecutionDto;
import com.finventory.model.DatabaseBackup;
import com.finventory.model.MigrationStageKey;
import com.finventory.service.DatabaseBackupService;
import com.finventory.service.MigrationOrchestrator;
import com.finventory.service.MigrationService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationService migrationService;
    private final MigrationOrchestrator migrationOrchestrator;
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseBackupService databaseBackupService;

    @GetMapping("/stages")
    public ResponseEntity<List<String>> getStages() {
        return ResponseEntity.ok(
                Arrays.stream(MigrationStageKey.values()).map(Enum::name).toList());
    }

    @GetMapping("/runs")
    public ResponseEntity<List<MigrationRunDto>> listRuns() {
        return ResponseEntity.ok(migrationService.listRuns());
    }

    @PostMapping("/runs")
    public ResponseEntity<MigrationRunDto> createRun(
            @RequestBody CreateMigrationRunRequest request, Principal principal) {
        String requestedBy = principal == null ? null : principal.getName();
        return ResponseEntity.ok(migrationService.createRun(request, requestedBy));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<MigrationRunDto> getRun(@PathVariable UUID id) {
        return ResponseEntity.ok(migrationService.getRun(id));
    }

    @GetMapping("/runs/{id}/stages")
    public ResponseEntity<List<MigrationStageExecutionDto>> getStagesForRun(@PathVariable UUID id) {
        return ResponseEntity.ok(migrationService.listStageExecutions(id));
    }

    @GetMapping("/runs/{id}/logs")
    public ResponseEntity<List<com.finventory.dto.MigrationLogEntryDto>> getLogsForRun(
            @PathVariable UUID id, @RequestParam(name = "limit", defaultValue = "200") int limit) {
        return ResponseEntity.ok(migrationService.listLogs(id, limit));
    }

    @PostMapping("/runs/{id}/stages/{stageKey}/execute")
    public ResponseEntity<MigrationStageExecutionDto> executeStage(
            @PathVariable UUID id, @PathVariable String stageKey) {
        MigrationStageKey key = MigrationStageKey.valueOf(stageKey.toUpperCase(Locale.ROOT));
        return ResponseEntity.ok(migrationService.executeStage(id, key));
    }

    @PostMapping("/runs/{id}/pipeline/full-safe/start")
    public ResponseEntity<MigrationPipelineProgressDto> startFullSafePipeline(
            @PathVariable UUID id,
            @RequestBody(required = false) MigrationPipelineStartRequest request) {
        return ResponseEntity.ok(migrationOrchestrator.startFullSafePipeline(id, request));
    }

    @GetMapping("/runs/{id}/pipeline/full-safe/progress")
    public ResponseEntity<MigrationPipelineProgressDto> getFullSafePipelineProgress(
            @PathVariable UUID id,
            @RequestParam(name = "preset", required = false) MigrationPipelinePreset preset) {
        return ResponseEntity.ok(migrationOrchestrator.getFullSafePipelineProgress(id, preset));
    }

    @PostMapping("/runs/{id}/pipeline/full-safe/resume")
    public ResponseEntity<MigrationPipelineProgressDto> resumeFullSafePipeline(
            @PathVariable UUID id,
            @RequestParam(name = "preset", required = false) MigrationPipelinePreset preset) {
        return ResponseEntity.ok(migrationOrchestrator.resumeFullSafePipeline(id, preset));
    }

    @PostMapping("/runs/{id}/pipeline/full-safe/retry/{stageKey}")
    public ResponseEntity<MigrationPipelineProgressDto> retryFailedStage(
            @PathVariable UUID id,
            @PathVariable String stageKey,
            @RequestParam(name = "preset", required = false) MigrationPipelinePreset preset) {
        return ResponseEntity.ok(migrationOrchestrator.retryFailedStage(id, stageKey, preset));
    }

    @PostMapping("/runs/{id}/pipeline/full-safe/cancel")
    public ResponseEntity<MigrationPipelineProgressDto> cancelPipeline(@PathVariable UUID id) {
        return ResponseEntity.ok(migrationOrchestrator.cancelPipeline(id));
    }

    @GetMapping("/db/migration-tables")
    public ResponseEntity<List<MigrationDbTableDto>> listMigrationTables() {
        List<String> tables =
                List.of(
                        "migration_runs",
                        "migration_stage_executions",
                        "migration_log_entries",
                        "migration_id_map");
        List<MigrationDbTableDto> result =
                tables.stream()
                        .map(
                                name -> {
                                    long rows = 0L;
                                    try {
                                        rows =
                                                jdbcTemplate.queryForObject(
                                                        "select count(*) from " + name, Long.class);
                                    } catch (Exception ignored) {
                                    }
                                    return new MigrationDbTableDto(name, rows);
                                })
                        .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/db/truncate")
    @Transactional
    public ResponseEntity<TruncateDatabaseResponse> truncateDatabase(
            @RequestBody(required = false) TruncateDatabaseRequest request) {
        boolean keepUsers =
                request == null
                        || request.getKeepUsers() == null
                        || Boolean.TRUE.equals(request.getKeepUsers());
        String confirmText = request == null ? null : request.getConfirmText();

        String expected = keepUsers ? "TRUNCATE_DATA" : "TRUNCATE_ALL";
        if (confirmText == null || !expected.equals(confirmText.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Confirmation text must be: " + expected);
        }

        Set<String> tables = listPublicTables();
        tables.remove("flyway_schema_history");
        if (keepUsers) {
            tables.remove("users");
        }

        if (!tables.isEmpty()) {
            tryPostgresTruncate(tables);
        }

        return ResponseEntity.ok(new TruncateDatabaseResponse(List.copyOf(tables), keepUsers));
    }

    private Set<String> listPublicTables() {
        Set<String> result = new LinkedHashSet<>();
        try (Connection c = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            String[] types = {"TABLE"};
            try (ResultSet rs = meta.getTables(c.getCatalog(), null, "%", types)) {
                while (rs.next()) {
                    String schema = rs.getString("TABLE_SCHEM");
                    String name = rs.getString("TABLE_NAME");
                    if (name == null) {
                        continue;
                    }
                    if (schema == null
                            || schema.equalsIgnoreCase("public")
                            || schema.equalsIgnoreCase("dbo")) {
                        result.add(name);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private void tryPostgresTruncate(Set<String> tables) {
        List<String> quotedTables =
                tables.stream().sorted().map(t -> "\"" + t.replace("\"", "\"\"") + "\"").toList();
        String joined = String.join(", ", quotedTables);
        try {
            jdbcTemplate.execute("TRUNCATE TABLE " + joined + " RESTART IDENTITY CASCADE");
            return;
        } catch (Exception ignored) {
        }

        for (String t : tables) {
            String quoted = "\"" + t.replace("\"", "\"\"") + "\"";
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + quoted + " RESTART IDENTITY CASCADE");
            } catch (Exception ignored) {
                jdbcTemplate.execute("TRUNCATE TABLE " + quoted);
            }
        }
    }

    public record MigrationDbTableDto(String table, long rowCount) {}

    public static final class TruncateDatabaseRequest {
        private Boolean keepUsers;
        private String confirmText;

        public Boolean getKeepUsers() {
            return keepUsers;
        }

        public void setKeepUsers(Boolean keepUsers) {
            this.keepUsers = keepUsers;
        }

        public String getConfirmText() {
            return confirmText;
        }

        public void setConfirmText(String confirmText) {
            this.confirmText = confirmText;
        }
    }

    public record TruncateDatabaseResponse(List<String> truncatedTables, boolean keepUsers) {}

    @GetMapping("/backups")
    public ResponseEntity<List<DatabaseBackupDto>> listDatabaseBackups() {
        return ResponseEntity.ok(databaseBackupService.listBackups());
    }

    @PostMapping("/backups")
    public ResponseEntity<DatabaseBackupDto> createDatabaseBackup(Principal principal) {
        String requestedBy = principal == null ? null : principal.getName();
        return ResponseEntity.ok(databaseBackupService.createBackup(requestedBy));
    }

    @GetMapping("/backups/{id}/download")
    public ResponseEntity<Resource> downloadDatabaseBackup(@PathVariable UUID id)
            throws IOException {
        DatabaseBackup backup = databaseBackupService.getBackup(id);
        Path file = databaseBackupService.resolveBackupFile(backup);

        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new IllegalArgumentException("Backup file not found");
        }

        String fileName =
                backup.getFileName() != null ? backup.getFileName() : file.getFileName().toString();
        long size = Files.size(file);
        InputStreamResource body = new InputStreamResource(Files.newInputStream(file));

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/sql"))
                .contentLength(size)
                .body(body);
    }
}
