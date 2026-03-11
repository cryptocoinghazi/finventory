package com.finventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.CreateMigrationRunRequest;
import com.finventory.dto.MigrationLogEntryDto;
import com.finventory.dto.MigrationRunDto;
import com.finventory.dto.MigrationStageExecutionDto;
import com.finventory.model.MigrationLogEntry;
import com.finventory.model.MigrationLogLevel;
import com.finventory.model.MigrationRun;
import com.finventory.model.MigrationRunStatus;
import com.finventory.model.MigrationStageExecution;
import com.finventory.model.MigrationStageKey;
import com.finventory.model.MigrationStageStatus;
import com.finventory.repository.MigrationLogEntryRepository;
import com.finventory.repository.MigrationRunRepository;
import com.finventory.repository.MigrationStageExecutionRepository;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MigrationService {
    private static final String DEFAULT_SOURCE_SYSTEM = "NEXOPOS";
    private static final int MAX_LOG_LIMIT = 1000;

    private final MigrationRunRepository runRepository;
    private final MigrationStageExecutionRepository stageExecutionRepository;
    private final MigrationLogEntryRepository logEntryRepository;
    private final NexoDumpAnalysisService dumpAnalysisService;
    private final NexoMigrationMasterDataStagesService masterDataStagesService;
    private final NexoOrdersMigrationService ordersMigrationService;
    private final ObjectMapper objectMapper;

    @Value("${application.migration.nexo-dump-path:../docs/nexo.sql}")
    private String nexoDumpPath;

    public List<MigrationRunDto> listRuns() {
        return runRepository.findAll().stream().map(this::mapToDto).toList();
    }

    public List<MigrationStageExecutionDto> listStageExecutions(UUID runId) {
        return stageExecutionRepository.findByRunIdOrderByStartedAtAsc(runId).stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<MigrationLogEntryDto> listLogs(UUID runId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LOG_LIMIT));
        return logEntryRepository.findByRunIdOrderByCreatedAtDesc(runId, PageRequest.of(0, safeLimit)).getContent()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public MigrationRunDto getRun(UUID id) {
        return mapToDto(runRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Run not found")));
    }

    @Transactional
    public void updateRunStatus(UUID runId, MigrationRunStatus status, boolean clearFinishedAt) {
        MigrationRun run =
                runRepository.findById(runId).orElseThrow(() -> new IllegalArgumentException("Run not found"));
        run.setStatus(status);
        if (clearFinishedAt) {
            run.setFinishedAt(null);
        } else if (status == MigrationRunStatus.COMPLETED
                || status == MigrationRunStatus.FAILED
                || status == MigrationRunStatus.CANCELLED) {
            run.setFinishedAt(OffsetDateTime.now());
        }
        runRepository.save(run);
    }

    @Transactional
    public void logMessage(
            UUID runId, MigrationStageKey stageKey, MigrationLogLevel level, String message, String details) {
        MigrationRun run =
                runRepository.findById(runId).orElseThrow(() -> new IllegalArgumentException("Run not found"));
        log(run, stageKey, level, message, details);
    }

    @Transactional
    public MigrationRunDto createRun(CreateMigrationRunRequest request, String requestedBy) {
        String sourceSystem = normalizeSourceSystem(request.getSourceSystem());
        boolean dryRun = request.getDryRun() == null || request.getDryRun();
        String sourceReference = normalizeSourceReference(request.getSourceReference());

        Long sourceIdMin = request.getSourceIdMin();
        Long sourceIdMax = request.getSourceIdMax();
        if (sourceIdMin != null && sourceIdMax != null && sourceIdMax < sourceIdMin) {
            throw new IllegalArgumentException("sourceIdMax must be >= sourceIdMin");
        }

        Integer scopeLimit = request.getLimit();
        if (scopeLimit != null && scopeLimit < 1) {
            scopeLimit = null;
        }

        MigrationRun run =
                MigrationRun.builder()
                        .sourceSystem(sourceSystem)
                        .sourceReference(sourceReference)
                        .dryRun(dryRun)
                        .status(MigrationRunStatus.CREATED)
                        .requestedBy(requestedBy)
                        .scopeSourceIdMin(sourceIdMin)
                        .scopeSourceIdMax(sourceIdMax)
                        .scopeLimit(scopeLimit)
                        .startedAt(OffsetDateTime.now())
                        .build();

        MigrationRun saved = runRepository.save(run);
        log(saved, null, MigrationLogLevel.INFO, "Migration run created", null);
        return mapToDto(saved);
    }

    @Transactional
    public MigrationStageExecutionDto executeStage(UUID runId, MigrationStageKey stageKey) {
        MigrationRun run =
                runRepository.findById(runId).orElseThrow(() -> new IllegalArgumentException("Run not found"));

        MigrationStageExecution stageExecution =
                stageExecutionRepository
                        .findByRunIdAndStageKey(runId, stageKey.name())
                        .orElseGet(
                                () ->
                                        MigrationStageExecution.builder()
                                                .run(run)
                                                .stageKey(stageKey.name())
                                                .status(MigrationStageStatus.CREATED)
                                                .startedAt(OffsetDateTime.now())
                                                .build());

        if (stageExecution.getStatus() == MigrationStageStatus.COMPLETED) {
            return mapToDto(stageExecutionRepository.save(stageExecution));
        }

        run.setStatus(MigrationRunStatus.RUNNING);
        stageExecution.setStatus(MigrationStageStatus.RUNNING);
        stageExecution.setStartedAt(OffsetDateTime.now());
        stageExecution.setFinishedAt(null);
        stageExecution.setErrorMessage(null);
        stageExecution.setStatsJson(null);

        runRepository.save(run);
        stageExecutionRepository.save(stageExecution);

        try {
            Path dumpPath = Path.of(run.getSourceReference());
            switch (stageKey) {
                case ANALYZE_SOURCE ->
                        setStats(stageExecution, dumpAnalysisService.analyzeSource(dumpPath));
                case IMPORT_UNITS -> setStats(stageExecution, masterDataStagesService.importUnits(run, dumpPath));
                case IMPORT_TAX_SLABS ->
                        setStats(stageExecution, masterDataStagesService.importTaxSlabs(run, dumpPath));
                case IMPORT_WAREHOUSES ->
                        setStats(stageExecution, masterDataStagesService.importWarehouses(run, dumpPath));
                case IMPORT_PARTIES -> setStats(stageExecution, masterDataStagesService.importParties(run, dumpPath));
                case IMPORT_ITEMS -> setStats(stageExecution, masterDataStagesService.importItems(run, dumpPath));
                case IMPORT_OPENING_STOCK ->
                        setStats(stageExecution, masterDataStagesService.importOpeningStock(run, dumpPath));
                case ANALYZE_ORDERS -> setStats(stageExecution, dumpAnalysisService.analyzeOrders(run, dumpPath));
                case IMPORT_SALES_PILOT ->
                        setStats(stageExecution, ordersMigrationService.importSalesPilot(run, dumpPath));
                case FINALIZE -> finalizeRun(run);
                default -> setStats(stageExecution, stageNotImplemented(run, stageKey));
            }

            stageExecution.setStatus(MigrationStageStatus.COMPLETED);
            stageExecution.setFinishedAt(OffsetDateTime.now());
            MigrationStageExecution savedStage = stageExecutionRepository.save(stageExecution);
            log(run, stageKey, MigrationLogLevel.INFO, "Stage completed", null);
            return mapToDto(savedStage);
        } catch (Exception e) {
            stageExecution.setStatus(MigrationStageStatus.FAILED);
            stageExecution.setFinishedAt(OffsetDateTime.now());
            stageExecution.setErrorMessage(e.getMessage());
            MigrationStageExecution savedStage = stageExecutionRepository.save(stageExecution);
            run.setStatus(MigrationRunStatus.FAILED);
            runRepository.save(run);
            log(run, stageKey, MigrationLogLevel.ERROR, "Stage failed", e.getMessage());
            return mapToDto(savedStage);
        }
    }

    private void setStats(MigrationStageExecution stageExecution, Map<String, Object> stats) throws Exception {
        stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
    }

    private String normalizeSourceSystem(String sourceSystem) {
        if (sourceSystem == null || sourceSystem.isBlank()) {
            return DEFAULT_SOURCE_SYSTEM;
        }
        return sourceSystem.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSourceReference(String sourceReference) {
        if (sourceReference == null || sourceReference.isBlank()) {
            return nexoDumpPath;
        }
        return sourceReference.trim();
    }

    private void finalizeRun(MigrationRun run) {
        run.setStatus(MigrationRunStatus.COMPLETED);
        run.setFinishedAt(OffsetDateTime.now());
        runRepository.save(run);
    }

    private Map<String, Object> stageNotImplemented(MigrationRun run, MigrationStageKey stageKey) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("implemented", false);
        stats.put("stage", stageKey.name());
        stats.put("dryRun", run.isDryRun());
        stats.put("sourceSystem", run.getSourceSystem());
        stats.put("sourceReference", run.getSourceReference());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("message", "Stage not implemented yet");
        return stats;
    }

    private void log(
            MigrationRun run,
            MigrationStageKey stageKey,
            MigrationLogLevel level,
            String message,
            String details) {
        MigrationLogEntry entry =
                MigrationLogEntry.builder()
                        .run(run)
                        .stageKey(stageKey == null ? null : stageKey.name())
                        .level(level)
                        .message(message)
                        .details(details)
                        .createdAt(OffsetDateTime.now())
                        .build();
        logEntryRepository.save(entry);
    }

    private MigrationRunDto mapToDto(MigrationRun run) {
        return MigrationRunDto.builder()
                .id(run.getId())
                .sourceSystem(run.getSourceSystem())
                .sourceReference(run.getSourceReference())
                .dryRun(run.isDryRun())
                .status(run.getStatus())
                .startedAt(run.getStartedAt())
                .finishedAt(run.getFinishedAt())
                .requestedBy(run.getRequestedBy())
                .scopeSourceIdMin(run.getScopeSourceIdMin())
                .scopeSourceIdMax(run.getScopeSourceIdMax())
                .scopeLimit(run.getScopeLimit())
                .build();
    }

    private MigrationStageExecutionDto mapToDto(MigrationStageExecution execution) {
        return MigrationStageExecutionDto.builder()
                .id(execution.getId())
                .runId(execution.getRun().getId())
                .stageKey(execution.getStageKey())
                .status(execution.getStatus())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .statsJson(execution.getStatsJson())
                .errorMessage(execution.getErrorMessage())
                .build();
    }

    private MigrationLogEntryDto mapToDto(MigrationLogEntry entry) {
        return MigrationLogEntryDto.builder()
                .id(entry.getId())
                .runId(entry.getRun().getId())
                .stageKey(entry.getStageKey())
                .level(entry.getLevel())
                .message(entry.getMessage())
                .details(entry.getDetails())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}

