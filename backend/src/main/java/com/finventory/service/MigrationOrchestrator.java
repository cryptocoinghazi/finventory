package com.finventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.MigrationPipelinePreset;
import com.finventory.dto.MigrationPipelineProgressDto;
import com.finventory.dto.MigrationPipelineStartRequest;
import com.finventory.dto.MigrationRunDto;
import com.finventory.dto.MigrationStageExecutionDto;
import com.finventory.model.MigrationLogLevel;
import com.finventory.model.MigrationRunStatus;
import com.finventory.model.MigrationStageKey;
import com.finventory.model.MigrationStageStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MigrationOrchestrator {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final MigrationService migrationService;
    private final ObjectMapper objectMapper;
    private final Executor migrationPipelineExecutor;

    private final ConcurrentHashMap<UUID, PipelineState> activePipelines = new ConcurrentHashMap<>();

    public MigrationPipelineProgressDto startFullSafePipeline(UUID runId, MigrationPipelineStartRequest request) {
        MigrationRunDto run = migrationService.getRun(runId);
        MigrationPipelinePreset preset = resolvePreset(run, request == null ? null : request.getPreset());
        validateStartRequest(run, preset, request);

        PipelineState existing = activePipelines.get(runId);
        if (existing != null && existing.active.get()) {
            throw new IllegalStateException("Pipeline is already running for this run");
        }

        PipelineState state = new PipelineState(preset);
        PipelineState prev = activePipelines.put(runId, state);
        if (prev != null) {
            prev.active.set(false);
            prev.cancelRequested.set(true);
        }

        migrationService.updateRunStatus(runId, MigrationRunStatus.RUNNING, true);
        migrationService.logMessage(runId, null, MigrationLogLevel.INFO, "Pipeline started", preset.name());

        migrationPipelineExecutor.execute(
                () -> {
                    try {
                        runPipeline(runId, preset, null);
                    } catch (Exception e) {
                        migrationService.logMessage(
                                runId, null, MigrationLogLevel.ERROR, "Pipeline crashed", e.getMessage());
                        migrationService.updateRunStatus(runId, MigrationRunStatus.FAILED, false);
                    } finally {
                        PipelineState s = activePipelines.get(runId);
                        if (s != null) {
                            s.active.set(false);
                            s.finishedAt = OffsetDateTime.now();
                        }
                    }
                });

        return getFullSafePipelineProgress(runId, preset);
    }

    public MigrationPipelineProgressDto resumeFullSafePipeline(UUID runId, MigrationPipelinePreset preset) {
        MigrationRunDto run = migrationService.getRun(runId);
        MigrationPipelinePreset resolvedPreset = resolvePreset(run, preset);

        PipelineState existing = activePipelines.get(runId);
        if (existing != null && existing.active.get()) {
            throw new IllegalStateException("Pipeline is already running for this run");
        }

        PipelineState state = new PipelineState(resolvedPreset);
        activePipelines.put(runId, state);

        migrationService.updateRunStatus(runId, MigrationRunStatus.RUNNING, true);
        migrationService.logMessage(runId, null, MigrationLogLevel.INFO, "Pipeline resumed", resolvedPreset.name());

        migrationPipelineExecutor.execute(
                () -> {
                    try {
                        runPipeline(runId, resolvedPreset, null);
                    } catch (Exception e) {
                        migrationService.logMessage(
                                runId, null, MigrationLogLevel.ERROR, "Pipeline crashed", e.getMessage());
                        migrationService.updateRunStatus(runId, MigrationRunStatus.FAILED, false);
                    } finally {
                        PipelineState s = activePipelines.get(runId);
                        if (s != null) {
                            s.active.set(false);
                            s.finishedAt = OffsetDateTime.now();
                        }
                    }
                });

        return getFullSafePipelineProgress(runId, resolvedPreset);
    }

    public MigrationPipelineProgressDto retryFailedStage(UUID runId, String stageKey, MigrationPipelinePreset preset) {
        MigrationRunDto run = migrationService.getRun(runId);
        MigrationPipelinePreset resolvedPreset = resolvePreset(run, preset);

        PipelineState existing = activePipelines.get(runId);
        if (existing != null && existing.active.get()) {
            throw new IllegalStateException("Pipeline is already running for this run");
        }

        MigrationStageKey key = MigrationStageKey.valueOf(stageKey.toUpperCase(Locale.ROOT));
        PipelineState state = new PipelineState(resolvedPreset);
        activePipelines.put(runId, state);

        migrationService.updateRunStatus(runId, MigrationRunStatus.RUNNING, true);
        migrationService.logMessage(
                runId, key, MigrationLogLevel.INFO, "Retrying stage and continuing pipeline", resolvedPreset.name());

        migrationPipelineExecutor.execute(
                () -> {
                    try {
                        runPipeline(runId, resolvedPreset, key);
                    } catch (Exception e) {
                        migrationService.logMessage(
                                runId, null, MigrationLogLevel.ERROR, "Pipeline crashed", e.getMessage());
                        migrationService.updateRunStatus(runId, MigrationRunStatus.FAILED, false);
                    } finally {
                        PipelineState s = activePipelines.get(runId);
                        if (s != null) {
                            s.active.set(false);
                            s.finishedAt = OffsetDateTime.now();
                        }
                    }
                });

        return getFullSafePipelineProgress(runId, resolvedPreset);
    }

    public MigrationPipelineProgressDto cancelPipeline(UUID runId) {
        PipelineState state = activePipelines.get(runId);
        if (state == null || !state.active.get()) {
            return getFullSafePipelineProgress(runId, null);
        }
        state.cancelRequested.set(true);
        migrationService.logMessage(
                runId,
                null,
                MigrationLogLevel.WARN,
                "Pipeline cancel requested",
                state.preset.name());
        return getFullSafePipelineProgress(runId, state.preset);
    }

    public MigrationPipelineProgressDto getFullSafePipelineProgress(UUID runId, MigrationPipelinePreset preset) {
        MigrationRunDto run = migrationService.getRun(runId);
        MigrationPipelinePreset resolvedPreset = resolvePreset(run, preset);

        List<String> plannedStages = getPresetStages(resolvedPreset).stream().map(Enum::name).toList();
        List<MigrationStageExecutionDto> executions = migrationService.listStageExecutions(runId);

        LinkedHashSet<String> completed = new LinkedHashSet<>();
        String failedStage = null;
        String currentStage = null;
        long warningsCount = 0;
        long errorsCount = 0;

        Map<String, MigrationStageExecutionDto> byStageKey = new java.util.HashMap<>();
        for (MigrationStageExecutionDto e : executions) {
            if (e.getStageKey() != null) {
                byStageKey.put(e.getStageKey(), e);
            }
        }

        for (String stage : plannedStages) {
            MigrationStageExecutionDto e = byStageKey.get(stage);
            if (e == null) {
                if (currentStage == null && failedStage == null) {
                    currentStage = stage;
                }
                continue;
            }
            if (e.getStatus() == MigrationStageStatus.RUNNING && currentStage == null) {
                currentStage = stage;
            }
            if (e.getStatus() == MigrationStageStatus.FAILED && failedStage == null) {
                failedStage = stage;
            }
            if (e.getStatus() == MigrationStageStatus.COMPLETED) {
                completed.add(stage);
            }

            Map<String, Object> stats = parseStats(e.getStatsJson());
            warningsCount += safeLong(stats.get("warnings"));
            errorsCount += safeLong(stats.get("errors"));
        }

        PipelineState runtime = activePipelines.get(runId);
        boolean active = runtime != null && runtime.active.get();

        if (!active) {
            if (failedStage != null) {
                currentStage = failedStage;
            } else if (completed.size() == plannedStages.size()) {
                currentStage = null;
            }
        }

        String summary;
        if (run.getStatus() == MigrationRunStatus.CANCELLED) {
            summary = "Cancelled";
        } else if (failedStage != null) {
            summary = "Failed at " + failedStage;
        } else if (completed.size() == plannedStages.size()) {
            summary = "Finished";
        } else if (active) {
            summary = "Running";
        } else {
            summary = "Idle";
        }

        return MigrationPipelineProgressDto.builder()
                .runId(runId)
                .runStatus(run.getStatus())
                .preset(resolvedPreset)
                .active(active)
                .startedAt(runtime != null ? runtime.startedAt : null)
                .finishedAt(runtime != null ? runtime.finishedAt : null)
                .currentStage(currentStage)
                .plannedStages(plannedStages)
                .completedStages(new ArrayList<>(completed))
                .failedStage(failedStage)
                .warningsCount(warningsCount)
                .errorsCount(errorsCount)
                .summary(summary)
                .build();
    }

    void runPipeline(UUID runId, MigrationPipelinePreset preset, MigrationStageKey forceStartStage) throws Exception {
        PipelineState state = activePipelines.get(runId);
        if (state == null) {
            return;
        }

        List<MigrationStageKey> planned = getPresetStages(preset);
        Map<MigrationStageKey, MigrationStageStatus> existing = loadExistingStageStatuses(runId);

        boolean startReached = forceStartStage == null;
        for (MigrationStageKey stageKey : planned) {
            if (!startReached) {
                if (stageKey == forceStartStage) {
                    startReached = true;
                } else {
                    continue;
                }
            }

            if (state.cancelRequested.get()) {
                migrationService.logMessage(runId, null, MigrationLogLevel.WARN, "Pipeline cancelled", preset.name());
                migrationService.updateRunStatus(runId, MigrationRunStatus.CANCELLED, false);
                return;
            }

            MigrationStageStatus status = existing.get(stageKey);
            if (status == MigrationStageStatus.RUNNING) {
                migrationService.logMessage(
                        runId, stageKey, MigrationLogLevel.ERROR, "Stage already RUNNING; cannot continue", null);
                migrationService.updateRunStatus(runId, MigrationRunStatus.FAILED, false);
                return;
            }
            if (status == MigrationStageStatus.COMPLETED && forceStartStage == null) {
                continue;
            }

            MigrationStageExecutionDto exec = migrationService.executeStage(runId, stageKey);
            existing.put(stageKey, exec.getStatus());

            if (exec.getStatus() == MigrationStageStatus.FAILED) {
                migrationService.logMessage(
                        runId,
                        stageKey,
                        MigrationLogLevel.ERROR,
                        "Pipeline stopped (stage failed)",
                        exec.getErrorMessage());
                migrationService.updateRunStatus(runId, MigrationRunStatus.FAILED, false);
                return;
            }

            Map<String, Object> stats = parseStats(exec.getStatsJson());
            Optional<String> blockingIssue = detectBlockingIssue(stageKey, stats);
            migrationService.logMessage(
                    runId,
                    stageKey,
                    MigrationLogLevel.INFO,
                    "Pipeline stage completed",
                    summarizeStats(stats));

            if (blockingIssue.isPresent()) {
                migrationService.logMessage(
                        runId,
                        stageKey,
                        MigrationLogLevel.ERROR,
                        "Pipeline stopped (blocking issues)",
                        blockingIssue.get());
                migrationService.updateRunStatus(runId, MigrationRunStatus.FAILED, false);
                return;
            }
        }

        migrationService.logMessage(runId, null, MigrationLogLevel.INFO, "Pipeline finished", preset.name());
        migrationService.updateRunStatus(runId, MigrationRunStatus.COMPLETED, false);
    }

    private Map<MigrationStageKey, MigrationStageStatus> loadExistingStageStatuses(UUID runId) {
        List<MigrationStageExecutionDto> executions = migrationService.listStageExecutions(runId);
        EnumMap<MigrationStageKey, MigrationStageStatus> result = new EnumMap<>(MigrationStageKey.class);
        for (MigrationStageExecutionDto e : executions) {
            if (e.getStageKey() == null) {
                continue;
            }
            try {
                MigrationStageKey key = MigrationStageKey.valueOf(e.getStageKey());
                result.put(key, e.getStatus());
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private MigrationPipelinePreset resolvePreset(MigrationRunDto run, MigrationPipelinePreset preset) {
        if (preset != null) {
            return preset;
        }
        if (run.isDryRun()) {
            return MigrationPipelinePreset.DRY_RUN_FULL_SAFE;
        }
        return MigrationPipelinePreset.REAL_PILOT_FULL_SAFE;
    }

    private void validateStartRequest(
            MigrationRunDto run, MigrationPipelinePreset preset, MigrationPipelineStartRequest request) {
        if (preset == MigrationPipelinePreset.DRY_RUN_FULL_SAFE && !run.isDryRun()) {
            throw new IllegalArgumentException("Dry-run pipeline requires a dry-run run");
        }
        if (preset == MigrationPipelinePreset.REAL_PILOT_FULL_SAFE && run.isDryRun()) {
            throw new IllegalArgumentException("Real pilot pipeline requires a write-enabled run (dryRun=false)");
        }
        if (preset == MigrationPipelinePreset.REAL_PILOT_FULL_SAFE) {
            boolean confirmed = request != null && Boolean.TRUE.equals(request.getConfirmed());
            if (!confirmed) {
                throw new IllegalArgumentException("Real pilot pipeline requires confirmed=true");
            }
        }
    }

    private List<MigrationStageKey> getPresetStages(MigrationPipelinePreset preset) {
        if (preset == MigrationPipelinePreset.REAL_PILOT_FULL_SAFE) {
            return List.of(
                    MigrationStageKey.IMPORT_UNITS,
                    MigrationStageKey.IMPORT_TAX_SLABS,
                    MigrationStageKey.IMPORT_PARTIES,
                    MigrationStageKey.IMPORT_ITEMS,
                    MigrationStageKey.IMPORT_OPENING_STOCK,
                    MigrationStageKey.IMPORT_SALES_PILOT,
                    MigrationStageKey.FINALIZE);
        }
        return List.of(
                MigrationStageKey.ANALYZE_SOURCE,
                MigrationStageKey.IMPORT_UNITS,
                MigrationStageKey.IMPORT_TAX_SLABS,
                MigrationStageKey.IMPORT_PARTIES,
                MigrationStageKey.IMPORT_ITEMS,
                MigrationStageKey.IMPORT_OPENING_STOCK,
                MigrationStageKey.ANALYZE_ORDERS,
                MigrationStageKey.IMPORT_SALES_PILOT,
                MigrationStageKey.FINALIZE);
    }

    private Optional<String> detectBlockingIssue(MigrationStageKey stageKey, Map<String, Object> stats) {
        if (stats == null || stats.isEmpty()) {
            return Optional.empty();
        }

        Object implemented = stats.get("implemented");
        if (implemented instanceof Boolean b && !b) {
            return Optional.of("Stage reports implemented=false");
        }

        long errors = safeLong(stats.get("errors"));
        if (errors > 0) {
            return Optional.of("errors=" + errors);
        }

        if (stageKey == MigrationStageKey.IMPORT_UNITS) {
            long missingUnitGroupMapping = safeLong(stats.get("missingUnitGroupMapping"));
            if (missingUnitGroupMapping > 0) {
                return Optional.of("missingUnitGroupMapping=" + missingUnitGroupMapping);
            }
        }

        if (stageKey == MigrationStageKey.IMPORT_SALES_PILOT) {
            long skippedMissingItems = safeLong(stats.get("skippedMissingItems"));
            long skippedMissingParty = safeLong(stats.get("skippedMissingParty"));
            if (skippedMissingItems > 0 || skippedMissingParty > 0) {
                return Optional.of(
                        "skippedMissingItems=" + skippedMissingItems + ", skippedMissingParty=" + skippedMissingParty);
            }
        }

        return Optional.empty();
    }

    private Map<String, Object> parseStats(String statsJson) {
        if (statsJson == null || statsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(statsJson, MAP_TYPE);
        } catch (Exception ignored) {
            return Map.of("raw", statsJson);
        }
    }

    private long safeLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (Exception ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private String summarizeStats(Map<String, Object> stats) {
        if (stats == null || stats.isEmpty()) {
            return null;
        }
        Map<String, Object> summary = new java.util.LinkedHashMap<>();
        for (String key
                : List.of(
                        "implemented",
                        "found",
                        "valid",
                        "inScope",
                        "created",
                        "updated",
                        "itemsCreated",
                        "itemsUpdated",
                        "fixedCustomersEnsured",
                        "customersCreated",
                        "customersEnsuredExisting",
                        "customersMappedExisting",
                        "customersWouldCreate",
                        "customersWouldMapExisting",
                        "vendorsCreatedFromCategories",
                        "vendorsWouldCreateFromCategories",
                        "vendorsLinkedExisting",
                        "vendorsAlreadyMapped",
                        "skippedOutOfScope",
                        "skippedOverLimit",
                        "alreadyMapped",
                        "linkedExisting",
                        "wouldCreate",
                        "wouldUpdate",
                        "warnings",
                        "errors",
                        "vendorMapped",
                        "vendorMissingMap",
                        "unsupportedDescriptionNonEmpty",
                        "unsupportedStatusPresent",
                        "unsupportedActivePresent",
                        "unsupportedCogsCandidatesPresent",
                        "stockAdjustmentsCreated",
                        "paidInvoices",
                        "pendingInvoices",
                        "invoiceLinesCreated",
                        "skippedMissingProductLines",
                        "markedPaid",
                        "eligiblePaidOrders",
                        "reconciliation",
                        "fallbackUsage")) {
            if (stats.containsKey(key)) {
                summary.put(key, stats.get(key));
            }
        }
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception ignored) {
            return summary.toString();
        }
    }

    private static final class PipelineState {
        private final MigrationPipelinePreset preset;
        private final OffsetDateTime startedAt;
        private volatile OffsetDateTime finishedAt;
        private final AtomicBoolean cancelRequested;
        private final AtomicBoolean active;

        private PipelineState(MigrationPipelinePreset preset) {
            this.preset = preset;
            this.startedAt = OffsetDateTime.now();
            this.cancelRequested = new AtomicBoolean(false);
            this.active = new AtomicBoolean(true);
        }
    }
}
