package com.finventory.controller;

import com.finventory.dto.CreateMigrationRunRequest;
import com.finventory.dto.MigrationRunDto;
import com.finventory.dto.MigrationStageExecutionDto;
import com.finventory.model.MigrationStageKey;
import com.finventory.service.MigrationService;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
