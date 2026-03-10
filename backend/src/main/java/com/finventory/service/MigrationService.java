package com.finventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finventory.dto.CreateMigrationRunRequest;
import com.finventory.dto.ItemDto;
import com.finventory.dto.MigrationLogEntryDto;
import com.finventory.dto.MigrationRunDto;
import com.finventory.dto.MigrationStageExecutionDto;
import com.finventory.dto.PartyDto;
import com.finventory.dto.SalesInvoiceDto;
import com.finventory.dto.SalesInvoiceLineDto;
import com.finventory.dto.StockAdjustmentDto;
import com.finventory.dto.TaxSlabDto;
import com.finventory.dto.WarehouseDto;
import com.finventory.model.InvoicePaymentStatus;
import com.finventory.model.MigrationIdMap;
import com.finventory.model.MigrationLogEntry;
import com.finventory.model.MigrationLogLevel;
import com.finventory.model.MigrationRun;
import com.finventory.model.MigrationRunStatus;
import com.finventory.model.MigrationStageExecution;
import com.finventory.model.MigrationStageKey;
import com.finventory.model.MigrationStageStatus;
import com.finventory.model.Party;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.TaxSlabRepository;
import com.finventory.repository.WarehouseRepository;
import com.finventory.repository.MigrationIdMapRepository;
import com.finventory.repository.MigrationLogEntryRepository;
import com.finventory.repository.MigrationRunRepository;
import com.finventory.repository.MigrationStageExecutionRepository;
import java.io.BufferedReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MigrationService {
    private static final String DEFAULT_SOURCE_SYSTEM = "NEXOPOS";
    private static final String ENTITY_TYPE_ITEM = "ITEM";
    private static final String ENTITY_TYPE_TAX_SLAB = "TAX_SLAB";
    private static final String ENTITY_TYPE_WAREHOUSE = "WAREHOUSE";
    private static final String ENTITY_TYPE_PARTY_CUSTOMER = "PARTY_CUSTOMER";
    private static final String ENTITY_TYPE_PARTY_VENDOR = "PARTY_VENDOR";
    private static final String ENTITY_TYPE_STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT";
    private static final String ENTITY_TYPE_SALES_INVOICE = "SALES_INVOICE";
    private static final String TABLE_PRODUCTS = "ns_nexopos_products";
    private static final String TABLE_UNITS = "ns_nexopos_units";
    private static final String TABLE_UNIT_GROUPS = "ns_nexopos_units_groups";
    private static final String TABLE_ORDERS = "ns_nexopos_orders";
    private static final String TABLE_ORDERS_PRODUCTS = "ns_nexopos_orders_products";
    private static final String TABLE_ORDERS_PAYMENTS = "ns_nexopos_orders_payments";
    private static final String TABLE_CUSTOMERS = "ns_nexopos_customers";
    private static final String TABLE_PROVIDERS = "ns_nexopos_providers";
    private static final String TABLE_TAXES = "ns_nexopos_taxes";
    private static final int ANALYZE_SAMPLE_ROWS = 3;
    private static final int MAX_LOG_LIMIT = 1000;
    private static final BigDecimal DEFAULT_UNIT_PRICE = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.ZERO;
    private static final String DEFAULT_UOM = "pcs";
    private static final int MAX_ERROR_SAMPLES = 25;
    private static final int MAX_CAPTURED_ORDER_IDS = 50_000;
    private static final int MAX_UOM_MAPPING_SAMPLE = 50;
    private static final int MAX_MISSING_UNIT_GROUP_SAMPLES = 25;

    private final MigrationRunRepository runRepository;
    private final MigrationStageExecutionRepository stageExecutionRepository;
    private final MigrationLogEntryRepository logEntryRepository;
    private final MigrationIdMapRepository idMapRepository;
    private final ItemRepository itemRepository;
    private final TaxSlabRepository taxSlabRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartyRepository partyRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final ItemService itemService;
    private final TaxSlabService taxSlabService;
    private final WarehouseService warehouseService;
    private final PartyService partyService;
    private final StockAdjustmentService stockAdjustmentService;
    private final SalesInvoiceService salesInvoiceService;
    private final ObjectMapper objectMapper;

    @Value("${application.migration.nexo-dump-path:../docs/nexo.sql}")
    private String nexoDumpPath;

    private static final class ImportItemsCounters {
        private final AtomicLong found = new AtomicLong();
        private final AtomicLong valid = new AtomicLong();
        private final AtomicLong skippedOutOfScope = new AtomicLong();
        private final AtomicLong skippedOverLimit = new AtomicLong();
        private final AtomicLong alreadyMapped = new AtomicLong();
        private final AtomicLong linkedExisting = new AtomicLong();
        private final AtomicLong created = new AtomicLong();
        private final AtomicLong updated = new AtomicLong();
        private final AtomicLong wouldCreate = new AtomicLong();
        private final AtomicLong wouldUpdate = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
    }

    private static final class ImportUnitsCounters {
        private final AtomicLong foundProducts = new AtomicLong();
        private final AtomicLong inScopeProducts = new AtomicLong();
        private final AtomicLong skippedOutOfScope = new AtomicLong();
        private final AtomicLong skippedOverLimit = new AtomicLong();
        private final AtomicLong skippedNotMapped = new AtomicLong();
        private final AtomicLong updated = new AtomicLong();
        private final AtomicLong wouldUpdate = new AtomicLong();
        private final AtomicLong unchanged = new AtomicLong();
        private final AtomicLong missingUnitGroupValue = new AtomicLong();
        private final AtomicLong missingUnitGroupMapping = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
    }

    private static final class ImportTaxSlabsCounters {
        private final AtomicLong found = new AtomicLong();
        private final AtomicLong inScope = new AtomicLong();
        private final AtomicLong skippedOutOfScope = new AtomicLong();
        private final AtomicLong skippedOverLimit = new AtomicLong();
        private final AtomicLong alreadyMapped = new AtomicLong();
        private final AtomicLong linkedExisting = new AtomicLong();
        private final AtomicLong created = new AtomicLong();
        private final AtomicLong wouldCreate = new AtomicLong();
        private final AtomicLong invalid = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
    }

    private static final class ImportWarehousesCounters {
        private final AtomicLong found = new AtomicLong();
        private final AtomicLong inScope = new AtomicLong();
        private final AtomicLong skippedOutOfScope = new AtomicLong();
        private final AtomicLong skippedOverLimit = new AtomicLong();
        private final AtomicLong alreadyMapped = new AtomicLong();
        private final AtomicLong linkedExisting = new AtomicLong();
        private final AtomicLong created = new AtomicLong();
        private final AtomicLong wouldCreate = new AtomicLong();
        private final AtomicLong invalid = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
    }

    private static final class ImportPartiesCounters {
        private final AtomicLong found = new AtomicLong();
        private final AtomicLong inScope = new AtomicLong();
        private final AtomicLong skippedOutOfScope = new AtomicLong();
        private final AtomicLong skippedOverLimit = new AtomicLong();
        private final AtomicLong alreadyMapped = new AtomicLong();
        private final AtomicLong linkedExisting = new AtomicLong();
        private final AtomicLong ambiguousExisting = new AtomicLong();
        private final AtomicLong created = new AtomicLong();
        private final AtomicLong wouldCreate = new AtomicLong();
        private final AtomicLong invalid = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
    }

    private static final class OrderHeader {
        private Long orderId;
        private String orderCode;
        private Long customerId;
        private String createdAt;
    }

    private static final class OrderLine {
        private Long productId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

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
        return logEntryRepository.findByRunIdOrderByCreatedAtDesc(runId, PageRequest.of(0, safeLimit))
                .getContent()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public MigrationRunDto getRun(UUID id) {
        return mapToDto(
                runRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Run not found")));
    }

    @Transactional
    public MigrationRunDto createRun(CreateMigrationRunRequest request, String requestedBy) {
        String sourceSystem =
                request.getSourceSystem() == null || request.getSourceSystem().isBlank()
                        ? DEFAULT_SOURCE_SYSTEM
                        : request.getSourceSystem().trim().toUpperCase(Locale.ROOT);

        boolean dryRun = request.getDryRun() == null || request.getDryRun();
        String sourceReference =
                request.getSourceReference() == null || request.getSourceReference().isBlank()
                        ? nexoDumpPath
                        : request.getSourceReference().trim();

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
            switch (stageKey) {
                case ANALYZE_SOURCE -> {
                    Map<String, Object> stats = analyzeSource(Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case IMPORT_UNITS -> {
                    Map<String, Object> stats = importUnits(run, Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case IMPORT_TAX_SLABS -> {
                    Map<String, Object> stats = importTaxSlabs(run, Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case IMPORT_WAREHOUSES -> {
                    Map<String, Object> stats = importWarehouses(run, Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case IMPORT_PARTIES -> {
                    Map<String, Object> stats = importParties(run, Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case IMPORT_OPENING_STOCK -> {
                    Map<String, Object> stats = importOpeningStock(run, Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case ANALYZE_ORDERS -> {
                    Map<String, Object> stats = analyzeOrders(run, Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case IMPORT_SALES_PILOT -> {
                    Map<String, Object> stats = importSalesPilot(run, Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case IMPORT_ITEMS -> {
                    Map<String, Object> stats = importItems(run, Path.of(run.getSourceReference()));
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
                case FINALIZE -> finalizeRun(run);
                default -> {
                    Map<String, Object> stats = stageNotImplemented(run, stageKey);
                    stageExecution.setStatsJson(objectMapper.writeValueAsString(stats));
                }
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

    private void finalizeRun(MigrationRun run) {
        run.setStatus(MigrationRunStatus.COMPLETED);
        run.setFinishedAt(OffsetDateTime.now());
        runRepository.save(run);
    }

    private Map<String, Object> analyzeSource(Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());

        stats.put(
                TABLE_CUSTOMERS,
                analyzeTable(
                        dumpPath,
                        TABLE_CUSTOMERS,
                        List.of("first_name", "last_name", "email", "phone"),
                        List.of("status")));

        stats.put(
                TABLE_PROVIDERS,
                analyzeTable(
                        dumpPath,
                        TABLE_PROVIDERS,
                        List.of("first_name", "last_name", "email", "phone", "amount_due", "amount_paid"),
                        List.of()));

        stats.put(
                "ns_nexopos_products",
                analyzeTable(
                        dumpPath,
                        TABLE_PRODUCTS,
                        List.of(
                                "name",
                                "sku",
                                "barcode",
                                "unit_group",
                                "category_id",
                                "tax_group_id",
                                "tax_type",
                                "tax_value",
                                "status",
                                "stock_management"),
                        List.of("status", "stock_management", "product_type", "type", "tax_type")));

        stats.put(
                TABLE_TAXES,
                analyzeTable(
                        dumpPath,
                        TABLE_TAXES,
                        List.of("name", "rate", "tax_group_id"),
                        List.of()));

        stats.put(
                "ns_nexopos_units",
                analyzeTable(
                        dumpPath,
                        TABLE_UNITS,
                        List.of("name", "identifier", "group_id", "base_unit"),
                        List.of()));

        stats.put(
                "ns_nexopos_orders",
                analyzeTable(
                        dumpPath,
                        "ns_nexopos_orders",
                        List.of(
                                "code",
                                "customer_id",
                                "subtotal",
                                "total",
                                "tax_value",
                                "payment_status",
                                "process_status",
                                "delivery_status",
                                "created_at"),
                        List.of("payment_status", "process_status", "delivery_status", "type")));

        stats.put(
                "ns_nexopos_orders_products",
                analyzeTable(
                        dumpPath,
                        "ns_nexopos_orders_products",
                        List.of("order_id", "product_id", "quantity", "price", "total_price"),
                        List.of()));

        stats.put(
                "ns_nexopos_orders_payments",
                analyzeTable(
                        dumpPath,
                        "ns_nexopos_orders_payments",
                        List.of("order_id", "value", "identifier", "created_at"),
                        List.of("identifier")));

        stats.put(
                "ns_nexopos_procurements",
                analyzeTable(
                        dumpPath,
                        "ns_nexopos_procurements",
                        List.of(
                                "name",
                                "provider_id",
                                "value",
                                "cost",
                                "tax_value",
                                "payment_status",
                                "delivery_status",
                                "invoice_reference",
                                "created_at"),
                        List.of("payment_status", "delivery_status")));

        stats.put(
                "ns_nexopos_procurements_products",
                analyzeTable(
                        dumpPath,
                        "ns_nexopos_procurements_products",
                        List.of(
                                "procurement_id",
                                "product_id",
                                "quantity",
                                "available_quantity",
                                "purchase_price",
                                "tax_type",
                                "tax_value"),
                        List.of("tax_type")));

        return stats;
    }

    private Map<String, Object> importUnits(MigrationRun run, Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        Map<Long, String> uomByGroupId = resolveUomByGroupId(dumpPath);

        ImportUnitsCounters counters = new ImportUnitsCounters();
        List<Long> missingUnitGroupMappingSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();
        Map<String, AtomicLong> uomCounts = new LinkedHashMap<>();

        applyUomFromProductsToItems(
                run,
                dumpPath,
                uomByGroupId,
                counters,
                uomCounts,
                missingUnitGroupMappingSamples,
                errorSamples);

        return buildImportUnitsStats(
                run,
                dumpPath,
                uomByGroupId,
                counters,
                uomCounts,
                missingUnitGroupMappingSamples,
                errorSamples);
    }

    private Map<String, Object> importTaxSlabs(MigrationRun run, Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        ImportTaxSlabsCounters counters = new ImportTaxSlabsCounters();
        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();

        forEachInsertRow(
                dumpPath,
                TABLE_TAXES,
                (columns, values) -> {
                    Long sourceId = asLong(getByColumn(columns, values, "id"));
                    if (sourceId == null) {
                        counters.invalid.incrementAndGet();
                        if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                            warningSamples.add("taxId=<NULL>");
                        }
                        return;
                    }

                    counters.found.incrementAndGet();

                    if (!isInScope(run, sourceId)) {
                        counters.skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = run.getScopeLimit();
                    if (scopeLimit != null && counters.inScope.get() >= scopeLimit) {
                        counters.skippedOverLimit.incrementAndGet();
                        return;
                    }

                    counters.inScope.incrementAndGet();

                    Optional<MigrationIdMap> existingMap =
                            idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                    run.getSourceSystem(), ENTITY_TYPE_TAX_SLAB, sourceId);
                    if (existingMap.isPresent()) {
                        counters.alreadyMapped.incrementAndGet();
                        return;
                    }

                    try {
                        BigDecimal rate = asBigDecimal(getByColumn(columns, values, "rate"), null);
                        if (rate == null) {
                            counters.invalid.incrementAndGet();
                            if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                                warningSamples.add("taxId=" + sourceId + " missing rate");
                            }
                            return;
                        }

                        String name = normalizeBlankToNull(getByColumn(columns, values, "name"));
                        TaxSlabDto dto =
                                TaxSlabDto.builder()
                                        .rate(rate)
                                        .description(name == null ? null : name)
                                        .build();

                        var existingByRate = taxSlabRepository.findByRate(rate);
                        if (existingByRate.isPresent()) {
                            counters.linkedExisting.incrementAndGet();
                            if (!run.isDryRun()) {
                                saveIdMap(
                                        run.getSourceSystem(),
                                        ENTITY_TYPE_TAX_SLAB,
                                        sourceId,
                                        existingByRate.get().getId());
                            }
                            return;
                        }

                        if (run.isDryRun()) {
                            counters.wouldCreate.incrementAndGet();
                            return;
                        }

                        TaxSlabDto created = taxSlabService.createTaxSlab(dto);
                        counters.created.incrementAndGet();
                        saveIdMap(run.getSourceSystem(), ENTITY_TYPE_TAX_SLAB, sourceId, created.getId());
                    } catch (Exception e) {
                        counters.errors.incrementAndGet();
                        if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                            errorSamples.add("taxId=" + sourceId + ": " + e.getMessage());
                        }
                    }
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("implemented", true);
        stats.put("stage", MigrationStageKey.IMPORT_TAX_SLABS.name());
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", run.isDryRun());
        stats.put("sourceSystem", run.getSourceSystem());
        stats.put("sourceReference", run.getSourceReference());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("insertStatements", countInsertStatements(dumpPath, TABLE_TAXES));

        stats.put("found", counters.found.get());
        stats.put("inScope", counters.inScope.get());
        stats.put("skippedOutOfScope", counters.skippedOutOfScope.get());
        stats.put("skippedOverLimit", counters.skippedOverLimit.get());
        stats.put("alreadyMapped", counters.alreadyMapped.get());
        stats.put("linkedExisting", counters.linkedExisting.get());
        stats.put("created", counters.created.get());
        stats.put("wouldCreate", counters.wouldCreate.get());
        stats.put("invalidRows", counters.invalid.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
        stats.put("errors", counters.errors.get());
        stats.put("errorSamples", errorSamples);
        stats.put("gstHandling", "Finventory stores total GST rate; invoices split CGST/SGST/IGST based on state codes.");

        log(
                run,
                MigrationStageKey.IMPORT_TAX_SLABS,
                MigrationLogLevel.INFO,
                "Tax slabs import finished",
                "found="
                        + counters.found.get()
                        + ", created="
                        + counters.created.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit());

        return stats;
    }

    private Map<String, Object> importWarehouses(MigrationRun run, Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        List<String> candidateTables =
                List.of(
                        "ns_nexopos_stores",
                        "ns_nexopos_warehouses",
                        "ns_nexopos_locations",
                        "ns_nexopos_branches");

        String selectedTable = null;
        for (String t : candidateTables) {
            if (countInsertStatements(dumpPath, t) > 0) {
                selectedTable = t;
                break;
            }
        }

        ImportWarehousesCounters counters = new ImportWarehousesCounters();
        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();

        if (selectedTable == null) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("implemented", true);
            stats.put("stage", MigrationStageKey.IMPORT_WAREHOUSES.name());
            stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
            stats.put("dryRun", run.isDryRun());
            stats.put("sourceSystem", run.getSourceSystem());
            stats.put("sourceReference", run.getSourceReference());
            stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
            stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
            stats.put("scopeLimit", run.getScopeLimit());
            stats.put("selectedSourceTable", null);
            stats.put("message", "No source warehouse/store table found in dump");
            stats.put("existingWarehouses", warehouseRepository.count());
            if (!run.isDryRun() && warehouseRepository.count() == 0) {
                WarehouseDto created =
                        warehouseService.createWarehouse(
                                WarehouseDto.builder().name("Main Warehouse").build());
                stats.put("createdDefaultWarehouseId", created.getId());
            }
            return stats;
        }

        forEachInsertRow(
                dumpPath,
                selectedTable,
                (columns, values) -> {
                    Long sourceId = asLong(getByColumn(columns, values, "id"));
                    if (sourceId == null) {
                        counters.invalid.incrementAndGet();
                        if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                            warningSamples.add("warehouseId=<NULL>");
                        }
                        return;
                    }

                    counters.found.incrementAndGet();

                    if (!isInScope(run, sourceId)) {
                        counters.skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = run.getScopeLimit();
                    if (scopeLimit != null && counters.inScope.get() >= scopeLimit) {
                        counters.skippedOverLimit.incrementAndGet();
                        return;
                    }

                    counters.inScope.incrementAndGet();

                    Optional<MigrationIdMap> existingMap =
                            idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                    run.getSourceSystem(), ENTITY_TYPE_WAREHOUSE, sourceId);
                    if (existingMap.isPresent()) {
                        counters.alreadyMapped.incrementAndGet();
                        return;
                    }

                    try {
                        String name =
                                firstNonBlank(
                                        columns,
                                        values,
                                        List.of("name", "title", "store_name", "label", "branch_name"));
                        if (name == null) {
                            name = "Warehouse-" + sourceId;
                        }

                        if (warehouseRepository.findByName(name).isPresent()) {
                            counters.linkedExisting.incrementAndGet();
                            if (!run.isDryRun()) {
                                saveIdMap(
                                        run.getSourceSystem(),
                                        ENTITY_TYPE_WAREHOUSE,
                                        sourceId,
                                        warehouseRepository.findByName(name).get().getId());
                            }
                            return;
                        }

                        String stateCode =
                                normalizeStateCode(
                                        firstNonBlank(
                                                columns,
                                                values,
                                                List.of("state_code", "state", "state_id")));
                        String location =
                                firstNonBlank(
                                        columns,
                                        values,
                                        List.of("location", "address", "city", "description"));

                        if (run.isDryRun()) {
                            counters.wouldCreate.incrementAndGet();
                            return;
                        }

                        WarehouseDto created =
                                warehouseService.createWarehouse(
                                        WarehouseDto.builder()
                                                .name(name)
                                                .stateCode(stateCode)
                                                .location(location)
                                                .build());
                        counters.created.incrementAndGet();
                        saveIdMap(run.getSourceSystem(), ENTITY_TYPE_WAREHOUSE, sourceId, created.getId());
                    } catch (Exception e) {
                        counters.errors.incrementAndGet();
                        if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                            errorSamples.add("warehouseId=" + sourceId + ": " + e.getMessage());
                        }
                    }
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("implemented", true);
        stats.put("stage", MigrationStageKey.IMPORT_WAREHOUSES.name());
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", run.isDryRun());
        stats.put("sourceSystem", run.getSourceSystem());
        stats.put("sourceReference", run.getSourceReference());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("selectedSourceTable", selectedTable);
        stats.put("insertStatements", countInsertStatements(dumpPath, selectedTable));
        stats.put("found", counters.found.get());
        stats.put("inScope", counters.inScope.get());
        stats.put("skippedOutOfScope", counters.skippedOutOfScope.get());
        stats.put("skippedOverLimit", counters.skippedOverLimit.get());
        stats.put("alreadyMapped", counters.alreadyMapped.get());
        stats.put("linkedExisting", counters.linkedExisting.get());
        stats.put("created", counters.created.get());
        stats.put("wouldCreate", counters.wouldCreate.get());
        stats.put("invalidRows", counters.invalid.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
        stats.put("errors", counters.errors.get());
        stats.put("errorSamples", errorSamples);

        log(
                run,
                MigrationStageKey.IMPORT_WAREHOUSES,
                MigrationLogLevel.INFO,
                "Warehouses import finished",
                "found="
                        + counters.found.get()
                        + ", created="
                        + counters.created.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit()
                        + ", table="
                        + selectedTable);

        return stats;
    }

    private Map<String, Object> importParties(MigrationRun run, Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("implemented", true);
        result.put("stage", MigrationStageKey.IMPORT_PARTIES.name());
        result.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        result.put("dryRun", run.isDryRun());
        result.put("sourceSystem", run.getSourceSystem());
        result.put("sourceReference", run.getSourceReference());
        result.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        result.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        result.put("scopeLimit", run.getScopeLimit());

        Map<String, Object> customers =
                importPartyTable(
                        run,
                        dumpPath,
                        TABLE_CUSTOMERS,
                        ENTITY_TYPE_PARTY_CUSTOMER,
                        Party.PartyType.CUSTOMER);
        Map<String, Object> vendors =
                importPartyTable(
                        run,
                        dumpPath,
                        TABLE_PROVIDERS,
                        ENTITY_TYPE_PARTY_VENDOR,
                        Party.PartyType.VENDOR);

        result.put("customers", customers);
        result.put("vendors", vendors);
        return result;
    }

    private Map<String, Object> importPartyTable(
            MigrationRun run,
            Path dumpPath,
            String tableName,
            String entityType,
            Party.PartyType partyType)
            throws Exception {
        ImportPartiesCounters counters = new ImportPartiesCounters();
        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();

        forEachInsertRow(
                dumpPath,
                tableName,
                (columns, values) -> {
                    Long sourceId = asLong(getByColumn(columns, values, "id"));
                    if (sourceId == null) {
                        counters.invalid.incrementAndGet();
                        if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                            warningSamples.add("partyId=<NULL>");
                        }
                        return;
                    }

                    counters.found.incrementAndGet();

                    if (!isInScope(run, sourceId)) {
                        counters.skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = run.getScopeLimit();
                    if (scopeLimit != null && counters.inScope.get() >= scopeLimit) {
                        counters.skippedOverLimit.incrementAndGet();
                        return;
                    }

                    counters.inScope.incrementAndGet();

                    Optional<MigrationIdMap> existingMap =
                            idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                    run.getSourceSystem(), entityType, sourceId);
                    if (existingMap.isPresent()) {
                        counters.alreadyMapped.incrementAndGet();
                        return;
                    }

                    try {
                        String firstName = normalizeBlankToNull(getByColumn(columns, values, "first_name"));
                        String lastName = normalizeBlankToNull(getByColumn(columns, values, "last_name"));
                        String fullName =
                                normalizeBlankToNull(getByColumn(columns, values, "name"));
                        String name =
                                fullName != null
                                        ? fullName
                                        : normalizeBlankToNull(
                                                (firstName == null ? "" : firstName)
                                                        + " "
                                                        + (lastName == null ? "" : lastName));
                        if (name == null) {
                            name = (partyType == Party.PartyType.CUSTOMER ? "Customer-" : "Vendor-") + sourceId;
                        }

                        String gstin =
                                normalizeBlankToNull(
                                        firstNonBlank(
                                                columns,
                                                values,
                                                List.of(
                                                        "gstin",
                                                        "gst_number",
                                                        "gst_number_id",
                                                        "vat",
                                                        "tax_number",
                                                        "tin")));
                        if (gstin != null) {
                            gstin = gstin.toUpperCase(Locale.ROOT);
                        }

                        String email = normalizeBlankToNull(getByColumn(columns, values, "email"));
                        String phone =
                                normalizeBlankToNull(
                                        firstNonBlank(columns, values, List.of("phone", "mobile")));
                        String address =
                                normalizeBlankToNull(
                                        firstNonBlank(
                                                columns,
                                                values,
                                                List.of(
                                                        "address",
                                                        "billing_address",
                                                        "shipping_address")));

                        String stateCode =
                                normalizeStateCode(
                                        firstNonBlank(columns, values, List.of("state_code", "state", "state_id")));
                        if (stateCode == null && gstin != null && gstin.length() >= 2) {
                            stateCode = normalizeStateCode(gstin.substring(0, 2));
                        }

                        if (gstin != null) {
                            Optional<Party> existingByGstin = partyRepository.findByGstin(gstin);
                            if (existingByGstin.isPresent()) {
                                counters.linkedExisting.incrementAndGet();
                                if (!run.isDryRun()) {
                                    saveIdMap(
                                            run.getSourceSystem(),
                                            entityType,
                                            sourceId,
                                            existingByGstin.get().getId());
                                }
                                return;
                            }
                        }

                        List<Party> existingByName = partyRepository.findByNameIgnoreCaseAndType(name, partyType);
                        if (existingByName.size() == 1) {
                            counters.linkedExisting.incrementAndGet();
                            if (!run.isDryRun()) {
                                saveIdMap(
                                        run.getSourceSystem(),
                                        entityType,
                                        sourceId,
                                        existingByName.get(0).getId());
                            }
                            return;
                        }
                        if (existingByName.size() > 1) {
                            counters.ambiguousExisting.incrementAndGet();
                            if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                                warningSamples.add(
                                        "partyId="
                                                + sourceId
                                                + " ambiguous existing matches for name="
                                                + name);
                            }
                            return;
                        }

                        PartyDto dto =
                                PartyDto.builder()
                                        .name(name)
                                        .type(partyType)
                                        .gstin(gstin)
                                        .stateCode(stateCode)
                                        .address(address)
                                        .phone(phone)
                                        .email(email)
                                        .build();

                        if (run.isDryRun()) {
                            counters.wouldCreate.incrementAndGet();
                            return;
                        }

                        PartyDto created = partyService.createParty(dto);
                        counters.created.incrementAndGet();
                        saveIdMap(run.getSourceSystem(), entityType, sourceId, created.getId());
                    } catch (Exception e) {
                        counters.errors.incrementAndGet();
                        if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                            errorSamples.add("partyId=" + sourceId + ": " + e.getMessage());
                        }
                    }
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("table", tableName);
        stats.put("partyType", partyType.name());
        stats.put("insertStatements", countInsertStatements(dumpPath, tableName));
        stats.put("found", counters.found.get());
        stats.put("inScope", counters.inScope.get());
        stats.put("skippedOutOfScope", counters.skippedOutOfScope.get());
        stats.put("skippedOverLimit", counters.skippedOverLimit.get());
        stats.put("alreadyMapped", counters.alreadyMapped.get());
        stats.put("linkedExisting", counters.linkedExisting.get());
        stats.put("ambiguousExisting", counters.ambiguousExisting.get());
        stats.put("created", counters.created.get());
        stats.put("wouldCreate", counters.wouldCreate.get());
        stats.put("invalidRows", counters.invalid.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
        stats.put("errors", counters.errors.get());
        stats.put("errorSamples", errorSamples);

        log(
                run,
                MigrationStageKey.IMPORT_PARTIES,
                MigrationLogLevel.INFO,
                "Parties import finished",
                "table="
                        + tableName
                        + ", partyType="
                        + partyType.name()
                        + ", found="
                        + counters.found.get()
                        + ", created="
                        + counters.created.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit());

        return stats;
    }

    private Map<String, Object> importOpeningStock(MigrationRun run, Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        UUID warehouseId = resolveDefaultWarehouseId(run);
        if (warehouseId == null) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("implemented", true);
            stats.put("stage", MigrationStageKey.IMPORT_OPENING_STOCK.name());
            stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
            stats.put("dryRun", run.isDryRun());
            stats.put("message", "No warehouse available; cannot import opening stock");
            return stats;
        }

        AtomicLong found = new AtomicLong();
        AtomicLong inScope = new AtomicLong();
        AtomicLong skippedOutOfScope = new AtomicLong();
        AtomicLong skippedOverLimit = new AtomicLong();
        AtomicLong skippedMissingItemMap = new AtomicLong();
        AtomicLong skippedMissingQuantity = new AtomicLong();
        AtomicLong skippedNonPositiveQuantity = new AtomicLong();
        AtomicLong alreadyMapped = new AtomicLong();
        AtomicLong created = new AtomicLong();
        AtomicLong wouldCreate = new AtomicLong();
        AtomicLong errors = new AtomicLong();

        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();

        forEachInsertRow(
                dumpPath,
                TABLE_PRODUCTS,
                (columns, values) -> {
                    Long productId = asLong(getByColumn(columns, values, "id"));
                    if (productId == null) {
                        return;
                    }
                    found.incrementAndGet();

                    if (!isInScope(run, productId)) {
                        skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = run.getScopeLimit();
                    if (scopeLimit != null && inScope.get() >= scopeLimit) {
                        skippedOverLimit.incrementAndGet();
                        return;
                    }
                    inScope.incrementAndGet();

                    Optional<MigrationIdMap> existingMap =
                            idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                    run.getSourceSystem(), ENTITY_TYPE_STOCK_ADJUSTMENT, productId);
                    if (existingMap.isPresent()) {
                        alreadyMapped.incrementAndGet();
                        return;
                    }

                    Optional<MigrationIdMap> itemMap =
                            idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                    run.getSourceSystem(), ENTITY_TYPE_ITEM, productId);
                    if (itemMap.isEmpty()) {
                        skippedMissingItemMap.incrementAndGet();
                        if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                            warningSamples.add("productId=" + productId + " missing item mapping");
                        }
                        return;
                    }

                    BigDecimal quantity =
                            asBigDecimal(
                                    firstNonBlank(
                                            columns,
                                            values,
                                            List.of("quantity", "available_quantity", "stock", "stock_quantity")),
                                    null);
                    if (quantity == null) {
                        skippedMissingQuantity.incrementAndGet();
                        return;
                    }
                    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                        skippedNonPositiveQuantity.incrementAndGet();
                        return;
                    }

                    if (run.isDryRun()) {
                        wouldCreate.incrementAndGet();
                        return;
                    }

                    try {
                        StockAdjustmentDto dto =
                                StockAdjustmentDto.builder()
                                        .adjustmentDate(LocalDate.now())
                                        .warehouseId(warehouseId)
                                        .itemId(itemMap.get().getTargetId())
                                        .quantity(quantity)
                                        .reason("Opening stock import (NexoPOS productId=" + productId + ")")
                                        .build();
                        StockAdjustmentDto createdAdj = stockAdjustmentService.createAdjustment(dto);
                        created.incrementAndGet();
                        saveIdMap(
                                run.getSourceSystem(),
                                ENTITY_TYPE_STOCK_ADJUSTMENT,
                                productId,
                                createdAdj.getId());
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                            errorSamples.add("productId=" + productId + ": " + e.getMessage());
                        }
                    }
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("implemented", true);
        stats.put("stage", MigrationStageKey.IMPORT_OPENING_STOCK.name());
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", run.isDryRun());
        stats.put("sourceSystem", run.getSourceSystem());
        stats.put("sourceReference", run.getSourceReference());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("warehouseId", warehouseId);
        stats.put("insertStatements", countInsertStatements(dumpPath, TABLE_PRODUCTS));
        stats.put("found", found.get());
        stats.put("inScope", inScope.get());
        stats.put("skippedOutOfScope", skippedOutOfScope.get());
        stats.put("skippedOverLimit", skippedOverLimit.get());
        stats.put("alreadyMapped", alreadyMapped.get());
        stats.put("skippedMissingItemMap", skippedMissingItemMap.get());
        stats.put("skippedMissingQuantity", skippedMissingQuantity.get());
        stats.put("skippedNonPositiveQuantity", skippedNonPositiveQuantity.get());
        stats.put("created", created.get());
        stats.put("wouldCreate", wouldCreate.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
        stats.put("errors", errors.get());
        stats.put("errorSamples", errorSamples);

        log(
                run,
                MigrationStageKey.IMPORT_OPENING_STOCK,
                MigrationLogLevel.INFO,
                "Opening stock import finished",
                "found="
                        + found.get()
                        + ", created="
                        + created.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit());

        return stats;
    }

    private Map<String, Object> importSalesPilot(MigrationRun run, Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        UUID warehouseId = resolveDefaultWarehouseId(run);
        if (warehouseId == null) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("implemented", true);
            stats.put("stage", MigrationStageKey.IMPORT_SALES_PILOT.name());
            stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
            stats.put("dryRun", run.isDryRun());
            stats.put("message", "No warehouse available; cannot import sales");
            return stats;
        }

        AtomicLong found = new AtomicLong();
        AtomicLong inScope = new AtomicLong();
        AtomicLong skippedOutOfScope = new AtomicLong();
        AtomicLong skippedOverLimit = new AtomicLong();
        AtomicLong skippedNotPaid = new AtomicLong();
        AtomicLong skippedMissingItems = new AtomicLong();
        AtomicLong skippedMissingParty = new AtomicLong();
        AtomicLong skippedNoLines = new AtomicLong();
        AtomicLong alreadyMapped = new AtomicLong();
        AtomicLong linkedExisting = new AtomicLong();
        AtomicLong created = new AtomicLong();
        AtomicLong wouldCreate = new AtomicLong();
        AtomicLong markedPaid = new AtomicLong();
        AtomicLong errors = new AtomicLong();

        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();

        Map<Long, OrderHeader> eligibleOrdersById = new LinkedHashMap<>();

        forEachInsertRow(
                dumpPath,
                TABLE_ORDERS,
                (columns, values) -> {
                    Long orderId = asLong(getByColumn(columns, values, "id"));
                    if (orderId == null) {
                        return;
                    }
                    found.incrementAndGet();

                    if (!isInScope(run, orderId)) {
                        skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = run.getScopeLimit();
                    if (scopeLimit != null && inScope.get() >= scopeLimit) {
                        skippedOverLimit.incrementAndGet();
                        return;
                    }

                    inScope.incrementAndGet();

                    String paymentStatus = normalizeBlankToNull(getByColumn(columns, values, "payment_status"));
                    if (!isPaidStatus(paymentStatus)) {
                        skippedNotPaid.incrementAndGet();
                        return;
                    }

                    OrderHeader header = new OrderHeader();
                    header.orderId = orderId;
                    header.orderCode =
                            normalizeBlankToNull(
                                    firstNonBlank(
                                            columns,
                                            values,
                                            List.of("code", "order_code", "reference", "invoice_reference")));
                    header.customerId = asLong(getByColumn(columns, values, "customer_id"));
                    header.createdAt =
                            normalizeBlankToNull(
                                    firstNonBlank(columns, values, List.of("created_at", "date", "created")));
                    eligibleOrdersById.put(orderId, header);
                });

        if (eligibleOrdersById.isEmpty()) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("implemented", true);
            stats.put("stage", MigrationStageKey.IMPORT_SALES_PILOT.name());
            stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
            stats.put("dryRun", run.isDryRun());
            stats.put("message", "No eligible paid orders found");
            stats.put("insertStatements", countInsertStatements(dumpPath, TABLE_ORDERS));
            return stats;
        }

        Map<Long, List<OrderLine>> linesByOrderId = new HashMap<>();
        forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PRODUCTS,
                (columns, values) -> {
                    Long orderId = asLong(getByColumn(columns, values, "order_id"));
                    if (orderId == null || !eligibleOrdersById.containsKey(orderId)) {
                        return;
                    }

                    Long productId = asLong(getByColumn(columns, values, "product_id"));
                    BigDecimal quantity =
                            asBigDecimal(getByColumn(columns, values, "quantity"), BigDecimal.ZERO);
                    BigDecimal unitPrice =
                            asBigDecimal(getByColumn(columns, values, "price"), null);
                    BigDecimal totalPrice =
                            asBigDecimal(getByColumn(columns, values, "total_price"), null);

                    OrderLine line = new OrderLine();
                    line.productId = productId;
                    line.quantity = quantity;
                    line.unitPrice = unitPrice;
                    line.totalPrice = totalPrice;

                    linesByOrderId.computeIfAbsent(orderId, ignored -> new ArrayList<>()).add(line);
                });

        Map<Long, BigDecimal> paymentsByOrderId = new HashMap<>();
        forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PAYMENTS,
                (columns, values) -> {
                    Long orderId = asLong(getByColumn(columns, values, "order_id"));
                    if (orderId == null || !eligibleOrdersById.containsKey(orderId)) {
                        return;
                    }

                    BigDecimal value = asBigDecimal(getByColumn(columns, values, "value"), BigDecimal.ZERO);
                    paymentsByOrderId.merge(orderId, value, BigDecimal::add);
                });

        for (OrderHeader header : eligibleOrdersById.values()) {
            Optional<MigrationIdMap> existingInvoiceMap =
                    idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                            run.getSourceSystem(), ENTITY_TYPE_SALES_INVOICE, header.orderId);
            if (existingInvoiceMap.isPresent()) {
                alreadyMapped.incrementAndGet();
                continue;
            }

            List<OrderLine> orderLines = linesByOrderId.getOrDefault(header.orderId, List.of());
            if (orderLines.isEmpty()) {
                skippedNoLines.incrementAndGet();
                if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                    warningSamples.add("orderId=" + header.orderId + " has no lines");
                }
                continue;
            }

            List<SalesInvoiceLineDto> invoiceLines = new ArrayList<>();
            boolean missingAnyItem = false;
            for (OrderLine l : orderLines) {
                if (l.productId == null) {
                    missingAnyItem = true;
                    continue;
                }
                Optional<MigrationIdMap> itemMap =
                        idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                run.getSourceSystem(), ENTITY_TYPE_ITEM, l.productId);
                if (itemMap.isEmpty()) {
                    missingAnyItem = true;
                    continue;
                }
                BigDecimal qty = l.quantity != null ? l.quantity : BigDecimal.ZERO;
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal unitPrice = l.unitPrice;
                if (unitPrice == null && l.totalPrice != null) {
                    unitPrice = l.totalPrice.divide(qty, 2, java.math.RoundingMode.HALF_UP);
                }
                if (unitPrice == null) {
                    unitPrice = DEFAULT_UNIT_PRICE;
                }

                invoiceLines.add(
                        SalesInvoiceLineDto.builder()
                                .itemId(itemMap.get().getTargetId())
                                .quantity(qty)
                                .unitPrice(unitPrice)
                                .build());
            }

            if (missingAnyItem) {
                skippedMissingItems.incrementAndGet();
                if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                    warningSamples.add("orderId=" + header.orderId + " missing item mappings");
                }
                continue;
            }

            if (invoiceLines.isEmpty()) {
                skippedNoLines.incrementAndGet();
                continue;
            }

            UUID partyId = resolveCustomerPartyId(run, header.customerId, warningSamples);
            if (partyId == null) {
                skippedMissingParty.incrementAndGet();
                if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                    warningSamples.add("orderId=" + header.orderId + " missing customer/party mapping");
                }
                continue;
            }

            LocalDate invoiceDate = parseLocalDateOrToday(header.createdAt);
            String invoiceNumber = buildLegacyInvoiceNumber(header);

            var existingByInvoiceNumber = salesInvoiceRepository.findByInvoiceNumber(invoiceNumber);
            if (existingByInvoiceNumber.isPresent()) {
                linkedExisting.incrementAndGet();
                if (!run.isDryRun()) {
                    saveIdMap(
                            run.getSourceSystem(),
                            ENTITY_TYPE_SALES_INVOICE,
                            header.orderId,
                            existingByInvoiceNumber.get().getId());
                }
                continue;
            }

            if (run.isDryRun()) {
                wouldCreate.incrementAndGet();
                continue;
            }

            try {
                SalesInvoiceDto createdInvoice =
                        salesInvoiceService.createSalesInvoice(
                                SalesInvoiceDto.builder()
                                        .invoiceNumber(invoiceNumber)
                                        .invoiceDate(invoiceDate)
                                        .partyId(partyId)
                                        .warehouseId(warehouseId)
                                        .paymentStatus(InvoicePaymentStatus.PENDING)
                                        .lines(invoiceLines)
                                        .build());

                created.incrementAndGet();
                saveIdMap(
                        run.getSourceSystem(),
                        ENTITY_TYPE_SALES_INVOICE,
                        header.orderId,
                        createdInvoice.getId());

                salesInvoiceService.applyPayment(
                        createdInvoice.getId(), InvoicePaymentStatus.PAID, BigDecimal.ZERO);
                markedPaid.incrementAndGet();
            } catch (Exception e) {
                errors.incrementAndGet();
                if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                    errorSamples.add("orderId=" + header.orderId + ": " + e.getMessage());
                }
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("implemented", true);
        stats.put("stage", MigrationStageKey.IMPORT_SALES_PILOT.name());
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", run.isDryRun());
        stats.put("sourceSystem", run.getSourceSystem());
        stats.put("sourceReference", run.getSourceReference());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("warehouseId", warehouseId);
        stats.put("eligiblePaidOrders", eligibleOrdersById.size());
        stats.put("insertStatementsOrders", countInsertStatements(dumpPath, TABLE_ORDERS));
        stats.put("insertStatementsOrderLines", countInsertStatements(dumpPath, TABLE_ORDERS_PRODUCTS));
        stats.put("insertStatementsPayments", countInsertStatements(dumpPath, TABLE_ORDERS_PAYMENTS));
        stats.put("found", found.get());
        stats.put("inScope", inScope.get());
        stats.put("skippedOutOfScope", skippedOutOfScope.get());
        stats.put("skippedOverLimit", skippedOverLimit.get());
        stats.put("skippedNotPaid", skippedNotPaid.get());
        stats.put("skippedMissingItems", skippedMissingItems.get());
        stats.put("skippedMissingParty", skippedMissingParty.get());
        stats.put("skippedNoLines", skippedNoLines.get());
        stats.put("alreadyMapped", alreadyMapped.get());
        stats.put("linkedExisting", linkedExisting.get());
        stats.put("created", created.get());
        stats.put("wouldCreate", wouldCreate.get());
        stats.put("markedPaid", markedPaid.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
        stats.put("errors", errors.get());
        stats.put("errorSamples", errorSamples);

        log(
                run,
                MigrationStageKey.IMPORT_SALES_PILOT,
                MigrationLogLevel.INFO,
                "Sales pilot import finished",
                "found="
                        + found.get()
                        + ", created="
                        + created.get()
                        + ", markedPaid="
                        + markedPaid.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit());

        return stats;
    }

    private String firstNonBlank(List<String> columns, List<String> values, List<String> candidateColumns) {
        for (String col : candidateColumns) {
            if (!columns.contains(col)) {
                continue;
            }
            String v = normalizeBlankToNull(getByColumn(columns, values, col));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private String normalizeStateCode(String value) {
        String v = normalizeBlankToNull(value);
        if (v == null) {
            return null;
        }
        StringBuilder digits = new StringBuilder(2);
        for (int i = 0; i < v.length() && digits.length() < 2; i++) {
            char ch = v.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            }
        }
        if (digits.length() == 2) {
            return digits.toString();
        }
        if (v.length() >= 2 && Character.isDigit(v.charAt(0)) && Character.isDigit(v.charAt(1))) {
            return v.substring(0, 2);
        }
        return null;
    }

    private UUID resolveDefaultWarehouseId(MigrationRun run) {
        List<com.finventory.model.Warehouse> warehouses = warehouseRepository.findAll(PageRequest.of(0, 1)).getContent();
        if (!warehouses.isEmpty()) {
            return warehouses.get(0).getId();
        }

        if (run.isDryRun()) {
            return null;
        }

        WarehouseDto created = warehouseService.createWarehouse(WarehouseDto.builder().name("Main Warehouse").build());
        return created.getId();
    }

    private UUID resolveCustomerPartyId(MigrationRun run, Long customerSourceId, List<String> warningSamples) {
        if (customerSourceId != null) {
            Optional<MigrationIdMap> map =
                    idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                            run.getSourceSystem(), ENTITY_TYPE_PARTY_CUSTOMER, customerSourceId);
            if (map.isPresent()) {
                return map.get().getTargetId();
            }
            if (warningSamples.size() < MAX_ERROR_SAMPLES) {
                warningSamples.add("customerId=" + customerSourceId + " missing customer mapping");
            }
        }

        List<Party> walkIns =
                partyRepository.findByNameIgnoreCaseAndType("Walk-in Customer", Party.PartyType.CUSTOMER);
        if (!walkIns.isEmpty()) {
            return walkIns.get(0).getId();
        }

        if (run.isDryRun()) {
            return null;
        }

        PartyDto created =
                partyService.createParty(
                        PartyDto.builder().name("Walk-in Customer").type(Party.PartyType.CUSTOMER).build());
        return created.getId();
    }

    private LocalDate parseLocalDateOrToday(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        String v = value.trim();
        if (v.length() >= 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
            try {
                return LocalDate.parse(v.substring(0, 10));
            } catch (Exception ignored) {
                return LocalDate.now();
            }
        }
        return LocalDate.now();
    }

    private boolean isPaidStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return false;
        }
        String p = paymentStatus.trim().toLowerCase(Locale.ROOT);
        return p.equals("paid")
                || p.startsWith("paid_")
                || p.equals("complete")
                || p.equals("completed")
                || p.equals("paid_out");
    }

    private String buildLegacyInvoiceNumber(OrderHeader header) {
        String base =
                header.orderCode != null && !header.orderCode.isBlank()
                        ? header.orderCode.trim()
                        : "ORDER-" + header.orderId;
        StringBuilder sb = new StringBuilder();
        sb.append("NEXO-");
        for (int i = 0; i < base.length(); i++) {
            char ch = base.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                sb.append(Character.toUpperCase(ch));
            } else if (ch == '-' || ch == '_' || ch == '/' || ch == ' ') {
                sb.append('-');
            }
            if (sb.length() >= 50) {
                break;
            }
        }
        if (sb.length() <= "NEXO-".length()) {
            return "NEXO-ORDER-" + header.orderId;
        }
        return sb.toString();
    }

    private void applyUomFromProductsToItems(
            MigrationRun run,
            Path dumpPath,
            Map<Long, String> uomByGroupId,
            ImportUnitsCounters counters,
            Map<String, AtomicLong> uomCounts,
            List<Long> missingUnitGroupMappingSamples,
            List<String> errorSamples)
            throws Exception {
        forEachInsertRow(
                dumpPath,
                TABLE_PRODUCTS,
                (columns, values) -> {
                    Long sourceId = asLong(getByColumn(columns, values, "id"));
                    if (sourceId == null) {
                        return;
                    }

                    counters.foundProducts.incrementAndGet();

                    if (!isInScope(run, sourceId)) {
                        counters.skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = run.getScopeLimit();
                    if (scopeLimit != null && counters.inScopeProducts.get() >= scopeLimit) {
                        counters.skippedOverLimit.incrementAndGet();
                        return;
                    }

                    counters.inScopeProducts.incrementAndGet();

                    Long unitGroupId = asLong(getByColumn(columns, values, "unit_group"));
                    if (unitGroupId == null) {
                        counters.missingUnitGroupValue.incrementAndGet();
                    } else if (!uomByGroupId.containsKey(unitGroupId)) {
                        counters.missingUnitGroupMapping.incrementAndGet();
                        if (missingUnitGroupMappingSamples.size() < MAX_MISSING_UNIT_GROUP_SAMPLES) {
                            missingUnitGroupMappingSamples.add(unitGroupId);
                        }
                    }

                    String uom =
                            unitGroupId == null
                                    ? DEFAULT_UOM
                                    : uomByGroupId.getOrDefault(unitGroupId, DEFAULT_UOM);
                    String uomKey = uom == null ? "<NULL>" : uom;
                    uomCounts.computeIfAbsent(uomKey, ignored -> new AtomicLong()).incrementAndGet();

                    Optional<MigrationIdMap> existingMap =
                            idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                    run.getSourceSystem(), ENTITY_TYPE_ITEM, sourceId);
                    if (existingMap.isEmpty()) {
                        counters.skippedNotMapped.incrementAndGet();
                        return;
                    }

                    UUID itemId = existingMap.get().getTargetId();
                    Optional<com.finventory.model.Item> itemOpt = itemRepository.findById(itemId);
                    if (itemOpt.isEmpty()) {
                        counters.errors.incrementAndGet();
                        if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                            errorSamples.add("productId=" + sourceId + " missing itemId=" + itemId);
                        }
                        return;
                    }

                    com.finventory.model.Item item = itemOpt.get();
                    if (uom != null && uom.equals(item.getUom())) {
                        counters.unchanged.incrementAndGet();
                        return;
                    }

                    if (run.isDryRun()) {
                        counters.wouldUpdate.incrementAndGet();
                        return;
                    }

                    item.setUom(uom == null ? DEFAULT_UOM : uom);
                    itemRepository.save(item);
                    counters.updated.incrementAndGet();
                });
    }

    private Map<String, Object> buildImportUnitsStats(
            MigrationRun run,
            Path dumpPath,
            Map<Long, String> uomByGroupId,
            ImportUnitsCounters counters,
            Map<String, AtomicLong> uomCounts,
            List<Long> missingUnitGroupMappingSamples,
            List<String> errorSamples) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", run.isDryRun());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("uomMappings", uomByGroupId.size());

        Map<Long, String> uomMappingSample = new LinkedHashMap<>();
        int sampleCount = 0;
        for (Map.Entry<Long, String> e : uomByGroupId.entrySet()) {
            uomMappingSample.put(e.getKey(), e.getValue());
            sampleCount++;
            if (sampleCount >= MAX_UOM_MAPPING_SAMPLE) {
                break;
            }
        }
        stats.put("uomMappingSample", uomMappingSample);

        stats.put("foundProducts", counters.foundProducts.get());
        stats.put("inScopeProducts", counters.inScopeProducts.get());
        stats.put("skippedOutOfScope", counters.skippedOutOfScope.get());
        stats.put("skippedOverLimit", counters.skippedOverLimit.get());
        stats.put("skippedNotMapped", counters.skippedNotMapped.get());
        stats.put("unchanged", counters.unchanged.get());
        stats.put("updated", counters.updated.get());
        stats.put("wouldUpdate", counters.wouldUpdate.get());
        stats.put("errors", counters.errors.get());
        stats.put("errorSamples", errorSamples);
        stats.put("missingUnitGroupValue", counters.missingUnitGroupValue.get());
        stats.put("missingUnitGroupMapping", counters.missingUnitGroupMapping.get());
        stats.put("missingUnitGroupMappingSamples", missingUnitGroupMappingSamples);

        Map<String, Long> uomCountResult = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> e : uomCounts.entrySet()) {
            uomCountResult.put(e.getKey(), e.getValue().get());
        }
        stats.put("uomCounts", uomCountResult);

        log(
                run,
                MigrationStageKey.IMPORT_UNITS,
                MigrationLogLevel.INFO,
                "Units import finished",
                "foundProducts="
                        + counters.foundProducts.get()
                        + ", updated="
                        + counters.updated.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit());

        return stats;
    }

    private Map<String, Object> analyzeOrders(MigrationRun run, Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        result.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        result.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        result.put("scopeLimit", run.getScopeLimit());

        Set<Long> capturedOrderIds = captureOrderIdsIfLimited(run);

        result.put("orders", analyzeOrdersTable(run, dumpPath, capturedOrderIds));
        result.put("ordersProducts", analyzeOrdersProducts(run, dumpPath, capturedOrderIds));
        result.put("ordersPayments", analyzeOrdersPayments(run, dumpPath, capturedOrderIds));

        return result;
    }

    private Set<Long> captureOrderIdsIfLimited(MigrationRun run) {
        Integer scopeLimit = run.getScopeLimit();
        if (scopeLimit == null) {
            return null;
        }
        int safe = Math.max(1, Math.min(scopeLimit, MAX_CAPTURED_ORDER_IDS));
        return new java.util.HashSet<>(safe * 2);
    }

    private Map<String, Object> analyzeOrdersTable(MigrationRun run, Path dumpPath, Set<Long> capturedOrderIds)
            throws Exception {
        AtomicLong found = new AtomicLong();
        AtomicLong inScope = new AtomicLong();
        AtomicLong skippedOutOfScope = new AtomicLong();
        AtomicLong skippedOverLimit = new AtomicLong();

        long[] minIdMaxId = {Long.MAX_VALUE, Long.MIN_VALUE};
        boolean[] idSeen = {false};

        BigDecimal[] sumSubtotal = {BigDecimal.ZERO};
        BigDecimal[] sumTotal = {BigDecimal.ZERO};
        BigDecimal[] sumTax = {BigDecimal.ZERO};

        String[] minCreatedAtMaxCreatedAt = {null, null};
        Map<String, Map<String, AtomicLong>> statuses = new LinkedHashMap<>();
        for (String col : List.of("payment_status", "process_status", "delivery_status", "type")) {
            statuses.put(col, new LinkedHashMap<>());
        }

        List<Map<String, Object>> sampleRows = new ArrayList<>();

        forEachInsertRow(
                dumpPath,
                TABLE_ORDERS,
                (columns, values) -> {
                    Long orderId = asLong(getByColumn(columns, values, "id"));
                    if (orderId == null) {
                        return;
                    }
                    found.incrementAndGet();

                    if (!isInScope(run, orderId)) {
                        skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = run.getScopeLimit();
                    if (scopeLimit != null && inScope.get() >= scopeLimit) {
                        skippedOverLimit.incrementAndGet();
                        return;
                    }

                    inScope.incrementAndGet();
                    if (capturedOrderIds != null) {
                        capturedOrderIds.add(orderId);
                    }

                    idSeen[0] = true;
                    if (orderId < minIdMaxId[0]) {
                        minIdMaxId[0] = orderId;
                    }
                    if (orderId > minIdMaxId[1]) {
                        minIdMaxId[1] = orderId;
                    }

                    BigDecimal subtotal = asBigDecimal(getByColumn(columns, values, "subtotal"), BigDecimal.ZERO);
                    BigDecimal total = asBigDecimal(getByColumn(columns, values, "total"), BigDecimal.ZERO);
                    BigDecimal tax = asBigDecimal(getByColumn(columns, values, "tax_value"), BigDecimal.ZERO);
                    sumSubtotal[0] = sumSubtotal[0].add(subtotal);
                    sumTotal[0] = sumTotal[0].add(total);
                    sumTax[0] = sumTax[0].add(tax);

                    String createdAt = normalizeBlankToNull(getByColumn(columns, values, "created_at"));
                    if (createdAt != null) {
                        if (minCreatedAtMaxCreatedAt[0] == null
                                || createdAt.compareTo(minCreatedAtMaxCreatedAt[0]) < 0) {
                            minCreatedAtMaxCreatedAt[0] = createdAt;
                        }
                        if (minCreatedAtMaxCreatedAt[1] == null
                                || createdAt.compareTo(minCreatedAtMaxCreatedAt[1]) > 0) {
                            minCreatedAtMaxCreatedAt[1] = createdAt;
                        }
                    }

                    for (Map.Entry<String, Map<String, AtomicLong>> e : statuses.entrySet()) {
                        String col = e.getKey();
                        if (!columns.contains(col)) {
                            continue;
                        }
                        String v = normalizeBlankToNull(getByColumn(columns, values, col));
                        String key = v == null ? "<NULL>" : v;
                        e.getValue().computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
                    }

                    if (sampleRows.size() < ANALYZE_SAMPLE_ROWS) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String col : List.of("id", "code", "customer_id", "total", "tax_value", "created_at")) {
                            if (columns.contains(col)) {
                                row.put(col, normalizeBlankToNull(getByColumn(columns, values, col)));
                            }
                        }
                        sampleRows.add(row);
                    }
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("insertStatements", countInsertStatements(dumpPath, TABLE_ORDERS));
        stats.put("found", found.get());
        stats.put("inScope", inScope.get());
        stats.put("skippedOutOfScope", skippedOutOfScope.get());
        stats.put("skippedOverLimit", skippedOverLimit.get());
        if (idSeen[0]) {
            stats.put("minId", minIdMaxId[0]);
            stats.put("maxId", minIdMaxId[1]);
        }
        stats.put("sumSubtotal", sumSubtotal[0].toPlainString());
        stats.put("sumTotal", sumTotal[0].toPlainString());
        stats.put("sumTaxValue", sumTax[0].toPlainString());
        if (minCreatedAtMaxCreatedAt[0] != null) {
            stats.put("minCreatedAt", minCreatedAtMaxCreatedAt[0]);
            stats.put("maxCreatedAt", minCreatedAtMaxCreatedAt[1]);
        }
        stats.put("capturedOrderIds", capturedOrderIds == null ? null : capturedOrderIds.size());

        Map<String, Map<String, Long>> statusResult = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, AtomicLong>> e : statuses.entrySet()) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (Map.Entry<String, AtomicLong> sc : e.getValue().entrySet()) {
                counts.put(sc.getKey(), sc.getValue().get());
            }
            statusResult.put(e.getKey(), counts);
        }
        stats.put("distinctStatuses", statusResult);
        stats.put("sampleRows", sampleRows);

        return stats;
    }

    private Map<String, Object> analyzeOrdersProducts(MigrationRun run, Path dumpPath, Set<Long> capturedOrderIds)
            throws Exception {
        AtomicLong found = new AtomicLong();
        AtomicLong matched = new AtomicLong();
        AtomicLong skippedOutOfScope = new AtomicLong();
        AtomicLong skippedNotCaptured = new AtomicLong();
        AtomicLong invalid = new AtomicLong();

        BigDecimal[] sumQuantity = {BigDecimal.ZERO};
        BigDecimal[] sumTotalPrice = {BigDecimal.ZERO};

        forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PRODUCTS,
                (columns, values) -> {
                    Long orderId = asLong(getByColumn(columns, values, "order_id"));
                    if (orderId == null) {
                        invalid.incrementAndGet();
                        return;
                    }
                    found.incrementAndGet();
                    if (!isInScope(run, orderId)) {
                        skippedOutOfScope.incrementAndGet();
                        return;
                    }
                    if (capturedOrderIds != null && !capturedOrderIds.contains(orderId)) {
                        skippedNotCaptured.incrementAndGet();
                        return;
                    }

                    matched.incrementAndGet();
                    BigDecimal qty = asBigDecimal(getByColumn(columns, values, "quantity"), BigDecimal.ZERO);
                    BigDecimal total =
                            asBigDecimal(getByColumn(columns, values, "total_price"), BigDecimal.ZERO);
                    sumQuantity[0] = sumQuantity[0].add(qty);
                    sumTotalPrice[0] = sumTotalPrice[0].add(total);
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("insertStatements", countInsertStatements(dumpPath, TABLE_ORDERS_PRODUCTS));
        stats.put("found", found.get());
        stats.put("matched", matched.get());
        stats.put("skippedOutOfScope", skippedOutOfScope.get());
        stats.put("skippedNotCaptured", skippedNotCaptured.get());
        stats.put("invalidRows", invalid.get());
        stats.put("sumQuantity", sumQuantity[0].toPlainString());
        stats.put("sumTotalPrice", sumTotalPrice[0].toPlainString());
        return stats;
    }

    private Map<String, Object> analyzeOrdersPayments(MigrationRun run, Path dumpPath, Set<Long> capturedOrderIds)
            throws Exception {
        AtomicLong found = new AtomicLong();
        AtomicLong matched = new AtomicLong();
        AtomicLong skippedOutOfScope = new AtomicLong();
        AtomicLong skippedNotCaptured = new AtomicLong();
        AtomicLong invalid = new AtomicLong();

        BigDecimal[] sumValue = {BigDecimal.ZERO};
        Map<String, AtomicLong> identifiers = new LinkedHashMap<>();

        forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PAYMENTS,
                (columns, values) -> {
                    Long orderId = asLong(getByColumn(columns, values, "order_id"));
                    if (orderId == null) {
                        invalid.incrementAndGet();
                        return;
                    }
                    found.incrementAndGet();
                    if (!isInScope(run, orderId)) {
                        skippedOutOfScope.incrementAndGet();
                        return;
                    }
                    if (capturedOrderIds != null && !capturedOrderIds.contains(orderId)) {
                        skippedNotCaptured.incrementAndGet();
                        return;
                    }

                    matched.incrementAndGet();
                    BigDecimal value = asBigDecimal(getByColumn(columns, values, "value"), BigDecimal.ZERO);
                    sumValue[0] = sumValue[0].add(value);

                    String identifier = normalizeBlankToNull(getByColumn(columns, values, "identifier"));
                    String key = identifier == null ? "<NULL>" : identifier;
                    identifiers.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("insertStatements", countInsertStatements(dumpPath, TABLE_ORDERS_PAYMENTS));
        stats.put("found", found.get());
        stats.put("matched", matched.get());
        stats.put("skippedOutOfScope", skippedOutOfScope.get());
        stats.put("skippedNotCaptured", skippedNotCaptured.get());
        stats.put("invalidRows", invalid.get());
        stats.put("sumValue", sumValue[0].toPlainString());

        Map<String, Long> identifierResult = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> e : identifiers.entrySet()) {
            identifierResult.put(e.getKey(), e.getValue().get());
        }
        stats.put("distinctIdentifiers", identifierResult);
        return stats;
    }

    private Map<String, Object> importItems(MigrationRun run, Path dumpPath) throws Exception {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }

        Map<Long, String> uomByGroupId = resolveUomByGroupId(dumpPath);
        Map<Long, BigDecimal> taxRateByTaxId = resolveTaxRateByTaxId(dumpPath);
        Map<Long, BigDecimal> taxRateByTaxGroupId = resolveTaxRateByTaxGroupId(dumpPath);
        Map<Long, Long> vendorSourceIdByProductId = resolveVendorSourceIdByProductId(dumpPath);
        List<String> errorSamples = new ArrayList<>();

        ImportItemsCounters counters = new ImportItemsCounters();

        forEachInsertRow(
                dumpPath,
                TABLE_PRODUCTS,
                (columns, values) -> {
                    Long sourceId = asLong(getByColumn(columns, values, "id"));
                    if (sourceId == null) {
                        return;
                    }

                    counters.found.incrementAndGet();

                    if (!isInScope(run, sourceId)) {
                        counters.skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = run.getScopeLimit();
                    if (scopeLimit != null && counters.valid.get() >= scopeLimit) {
                        counters.skippedOverLimit.incrementAndGet();
                        return;
                    }

                    counters.valid.incrementAndGet();

                    try {
                        String name = normalizeBlankToNull(getByColumn(columns, values, "name"));
                        String sku = normalizeBlankToNull(getByColumn(columns, values, "sku"));
                        String barcode = normalizeBlankToNull(getByColumn(columns, values, "barcode"));
                        Long unitGroupId = asLong(getByColumn(columns, values, "unit_group"));
                        Long taxGroupId = asLong(getByColumn(columns, values, "tax_group_id"));
                        Long taxId = asLong(getByColumn(columns, values, "tax_id"));
                        BigDecimal taxRate = null;
                        if (taxGroupId != null && taxRateByTaxGroupId.containsKey(taxGroupId)) {
                            taxRate = taxRateByTaxGroupId.get(taxGroupId);
                        } else if (taxId != null && taxRateByTaxId.containsKey(taxId)) {
                            taxRate = taxRateByTaxId.get(taxId);
                        } else {
                            taxRate =
                                    asBigDecimal(
                                            getByColumn(columns, values, "tax_value"), DEFAULT_TAX_RATE);
                        }

                        Long vendorSourceId =
                                asLong(
                                        firstNonBlank(
                                                columns,
                                                values,
                                                List.of("provider_id", "vendor_id", "supplier_id")));
                        if (vendorSourceId == null) {
                            vendorSourceId = vendorSourceIdByProductId.get(sourceId);
                        }

                        String code = chooseItemCode(sku, barcode, sourceId);
                        String uom =
                                unitGroupId == null
                                        ? DEFAULT_UOM
                                        : uomByGroupId.getOrDefault(unitGroupId, DEFAULT_UOM);

                        Optional<MigrationIdMap> existingMap =
                                idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                        run.getSourceSystem(), ENTITY_TYPE_ITEM, sourceId);
                        com.finventory.model.Item targetItem = null;
                        if (existingMap.isPresent()) {
                            counters.alreadyMapped.incrementAndGet();
                            targetItem = itemRepository.findById(existingMap.get().getTargetId()).orElse(null);
                        } else {
                            Optional<com.finventory.model.Item> existingItem = itemRepository.findByCode(code);
                            if (existingItem.isPresent()) {
                                counters.linkedExisting.incrementAndGet();
                                targetItem = existingItem.get();
                                if (!run.isDryRun()) {
                                    saveIdMap(
                                            run.getSourceSystem(),
                                            ENTITY_TYPE_ITEM,
                                            sourceId,
                                            existingItem.get().getId());
                                }
                            }
                        }

                        UUID vendorId = null;
                        if (vendorSourceId != null) {
                            Optional<MigrationIdMap> vendorMap =
                                    idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                                            run.getSourceSystem(), ENTITY_TYPE_PARTY_VENDOR, vendorSourceId);
                            if (vendorMap.isPresent()) {
                                vendorId = vendorMap.get().getTargetId();
                            }
                        }

                        if (targetItem != null) {
                            boolean changed = false;
                            if (taxRate != null && taxRate.compareTo(targetItem.getTaxRate()) != 0) {
                                targetItem.setTaxRate(taxRate);
                                changed = true;
                            }
                            if (vendorId != null) {
                                Party vendor = partyRepository.findById(vendorId).orElse(null);
                                if (vendor != null
                                        && (targetItem.getVendor() == null
                                                || !vendor.getId().equals(targetItem.getVendor().getId()))) {
                                    targetItem.setVendor(vendor);
                                    changed = true;
                                }
                            }
                            if (uom != null && !uom.equals(targetItem.getUom())) {
                                targetItem.setUom(uom);
                                changed = true;
                            }

                            if (!changed) {
                                return;
                            }

                            if (run.isDryRun()) {
                                counters.wouldUpdate.incrementAndGet();
                                return;
                            }

                            itemRepository.save(targetItem);
                            counters.updated.incrementAndGet();
                            return;
                        }

                        ItemDto dto =
                                ItemDto.builder()
                                        .name(name == null ? code : name)
                                        .code(code)
                                        .barcode(barcode)
                                        .taxRate(taxRate)
                                        .unitPrice(DEFAULT_UNIT_PRICE)
                                        .uom(uom)
                                        .build();

                        if (dto.getBarcode() != null
                                && itemRepository.existsByBarcode(dto.getBarcode())) {
                            dto.setBarcode(null);
                        }

                        if (run.isDryRun()) {
                            counters.wouldCreate.incrementAndGet();
                            return;
                        }

                        ItemDto createdItem = itemService.createItem(dto);
                        if (vendorId != null) {
                            Party vendor = partyRepository.findById(vendorId).orElse(null);
                            if (vendor != null) {
                                com.finventory.model.Item createdEntity =
                                        itemRepository.findById(createdItem.getId()).orElse(null);
                                if (createdEntity != null) {
                                    createdEntity.setVendor(vendor);
                                    itemRepository.save(createdEntity);
                                }
                            }
                        }
                        counters.created.incrementAndGet();
                        saveIdMap(run.getSourceSystem(), ENTITY_TYPE_ITEM, sourceId, createdItem.getId());
                    } catch (Exception e) {
                        counters.errors.incrementAndGet();
                        if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                            errorSamples.add("productId=" + sourceId + ": " + e.getMessage());
                        }
                    }
                });

        return buildImportItemsStats(
                run,
                dumpPath,
                counters,
                errorSamples);
    }

    private Map<Long, BigDecimal> resolveTaxRateByTaxId(Path dumpPath) throws Exception {
        Map<Long, BigDecimal> rateById = new HashMap<>();
        forEachInsertRow(
                dumpPath,
                TABLE_TAXES,
                (columns, values) -> {
                    Long id = asLong(getByColumn(columns, values, "id"));
                    BigDecimal rate = asBigDecimal(getByColumn(columns, values, "rate"), null);
                    if (id != null && rate != null) {
                        rateById.put(id, rate);
                    }
                });
        return rateById;
    }

    private Map<Long, BigDecimal> resolveTaxRateByTaxGroupId(Path dumpPath) throws Exception {
        Map<Long, BigDecimal> rateByGroupId = new HashMap<>();
        forEachInsertRow(
                dumpPath,
                TABLE_TAXES,
                (columns, values) -> {
                    Long groupId = asLong(getByColumn(columns, values, "tax_group_id"));
                    BigDecimal rate = asBigDecimal(getByColumn(columns, values, "rate"), null);
                    if (groupId == null || rate == null) {
                        return;
                    }
                    rateByGroupId.merge(groupId, rate, BigDecimal::add);
                });
        return rateByGroupId;
    }

    private Map<Long, Long> resolveVendorSourceIdByProductId(Path dumpPath) throws Exception {
        long procurementsInsertStatements = countInsertStatements(dumpPath, "ns_nexopos_procurements");
        long procurementsProductsInsertStatements =
                countInsertStatements(dumpPath, "ns_nexopos_procurements_products");
        if (procurementsInsertStatements == 0 || procurementsProductsInsertStatements == 0) {
            return Map.of();
        }

        Map<Long, Long> providerIdByProcurementId = new HashMap<>();
        forEachInsertRow(
                dumpPath,
                "ns_nexopos_procurements",
                (columns, values) -> {
                    Long procurementId = asLong(getByColumn(columns, values, "id"));
                    Long providerId = asLong(getByColumn(columns, values, "provider_id"));
                    if (procurementId != null && providerId != null) {
                        providerIdByProcurementId.put(procurementId, providerId);
                    }
                });

        Map<Long, Map<Long, AtomicLong>> providerCountsByProductId = new HashMap<>();
        forEachInsertRow(
                dumpPath,
                "ns_nexopos_procurements_products",
                (columns, values) -> {
                    Long procurementId = asLong(getByColumn(columns, values, "procurement_id"));
                    Long productId = asLong(getByColumn(columns, values, "product_id"));
                    if (procurementId == null || productId == null) {
                        return;
                    }
                    Long providerId = providerIdByProcurementId.get(procurementId);
                    if (providerId == null) {
                        return;
                    }

                    providerCountsByProductId
                            .computeIfAbsent(productId, ignored -> new HashMap<>())
                            .computeIfAbsent(providerId, ignored -> new AtomicLong())
                            .incrementAndGet();
                });

        Map<Long, Long> bestProviderByProductId = new HashMap<>();
        for (Map.Entry<Long, Map<Long, AtomicLong>> entry : providerCountsByProductId.entrySet()) {
            Long productId = entry.getKey();
            Long bestProviderId = null;
            long bestCount = -1L;
            for (Map.Entry<Long, AtomicLong> c : entry.getValue().entrySet()) {
                long count = c.getValue().get();
                if (count > bestCount) {
                    bestCount = count;
                    bestProviderId = c.getKey();
                }
            }
            if (bestProviderId != null) {
                bestProviderByProductId.put(productId, bestProviderId);
            }
        }
        return bestProviderByProductId;
    }

    private Map<String, Object> buildImportItemsStats(
            MigrationRun run, Path dumpPath, ImportItemsCounters counters, List<String> errorSamples) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", run.isDryRun());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("found", counters.found.get());
        stats.put("valid", counters.valid.get());
        stats.put(
                "skipped",
                counters.skippedOutOfScope.get()
                        + counters.skippedOverLimit.get()
                        + counters.alreadyMapped.get()
                        + counters.linkedExisting.get());
        stats.put("skippedOutOfScope", counters.skippedOutOfScope.get());
        stats.put("skippedOverLimit", counters.skippedOverLimit.get());
        stats.put("alreadyMapped", counters.alreadyMapped.get());
        stats.put("linkedExisting", counters.linkedExisting.get());
        stats.put("created", counters.created.get());
        stats.put("updated", counters.updated.get());
        stats.put("wouldCreate", counters.wouldCreate.get());
        stats.put("wouldUpdate", counters.wouldUpdate.get());
        stats.put("errors", counters.errors.get());
        stats.put("errorSamples", errorSamples);

        log(
                run,
                MigrationStageKey.IMPORT_ITEMS,
                MigrationLogLevel.INFO,
                "Items import finished",
                "found="
                        + counters.found.get()
                        + ", created="
                        + counters.created.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit());

        return stats;
    }

    private boolean isInScope(MigrationRun run, Long sourceId) {
        Long min = run.getScopeSourceIdMin();
        if (min != null && sourceId < min) {
            return false;
        }
        Long max = run.getScopeSourceIdMax();
        if (max != null && sourceId > max) {
            return false;
        }
        return true;
    }

    private void saveIdMap(String sourceSystem, String entityType, Long sourceId, UUID targetId) {
        MigrationIdMap mapping =
                MigrationIdMap.builder()
                        .sourceSystem(sourceSystem)
                        .entityType(entityType)
                        .sourceId(sourceId)
                        .targetId(targetId)
                        .build();
        idMapRepository.save(mapping);
    }

    private Map<String, Object> analyzeTable(
            Path dumpPath, String tableName, List<String> importantColumns, List<String> statusColumns)
            throws Exception {
        long insertStatements = countInsertStatements(dumpPath, tableName);

        long[] tupleCount = {0L};
        long[] minIdMaxId = {Long.MAX_VALUE, Long.MIN_VALUE};
        boolean[] idSeen = {false};
        List<String> insertColumns = new ArrayList<>();

        Map<String, AtomicLong> nullCounts = new LinkedHashMap<>();
        for (String col : importantColumns) {
            nullCounts.put(col, new AtomicLong());
        }

        Map<String, Map<String, AtomicLong>> distinctStatuses = new LinkedHashMap<>();
        for (String col : statusColumns) {
            distinctStatuses.put(col, new LinkedHashMap<>());
        }

        List<String> sampleColumns = new ArrayList<>();
        sampleColumns.add("id");
        for (String col : importantColumns) {
            if (!sampleColumns.contains(col)) {
                sampleColumns.add(col);
            }
        }
        for (String col : statusColumns) {
            if (!sampleColumns.contains(col)) {
                sampleColumns.add(col);
            }
        }

        List<Map<String, Object>> sampleRows = new ArrayList<>();

        forEachInsertRow(
                dumpPath,
                tableName,
                (columns, values) -> {
                    if (insertColumns.isEmpty() && !columns.isEmpty()) {
                        insertColumns.addAll(columns);
                    }
                    tupleCount[0]++;

                    Long id = asLong(getByColumn(columns, values, "id"));
                    if (id != null) {
                        idSeen[0] = true;
                        if (id < minIdMaxId[0]) {
                            minIdMaxId[0] = id;
                        }
                        if (id > minIdMaxId[1]) {
                            minIdMaxId[1] = id;
                        }
                    }

                    for (String col : importantColumns) {
                        if (!columns.contains(col)) {
                            continue;
                        }
                        String v = normalizeBlankToNull(getByColumn(columns, values, col));
                        if (v == null) {
                            nullCounts.get(col).incrementAndGet();
                        }
                    }

                    for (String col : statusColumns) {
                        if (!columns.contains(col)) {
                            continue;
                        }
                        String v = normalizeBlankToNull(getByColumn(columns, values, col));
                        String key = v == null ? "<NULL>" : v;
                        distinctStatuses
                                .get(col)
                                .computeIfAbsent(key, ignored -> new AtomicLong())
                                .incrementAndGet();
                    }

                    if (sampleRows.size() < ANALYZE_SAMPLE_ROWS) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String col : sampleColumns) {
                            if (columns.contains(col)) {
                                row.put(col, normalizeBlankToNull(getByColumn(columns, values, col)));
                            }
                        }
                        sampleRows.add(row);
                    }
                });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("insertStatements", insertStatements);
        result.put("tuples", tupleCount[0]);
        result.put("insertColumns", insertColumns);
        if (idSeen[0]) {
            result.put("minId", minIdMaxId[0]);
            result.put("maxId", minIdMaxId[1]);
        }

        Map<String, Long> nullCountResult = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> e : nullCounts.entrySet()) {
            nullCountResult.put(e.getKey(), e.getValue().get());
        }
        result.put("nullCount", nullCountResult);

        Map<String, Map<String, Long>> distinctResult = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, AtomicLong>> e : distinctStatuses.entrySet()) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (Map.Entry<String, AtomicLong> sc : e.getValue().entrySet()) {
                counts.put(sc.getKey(), sc.getValue().get());
            }
            distinctResult.put(e.getKey(), counts);
        }
        result.put("distinctStatuses", distinctResult);

        List<String> missingImportantColumns = new ArrayList<>();
        for (String col : importantColumns) {
            if (!insertColumns.contains(col)) {
                missingImportantColumns.add(col);
            }
        }
        result.put("missingImportantColumns", missingImportantColumns);

        List<String> missingStatusColumns = new ArrayList<>();
        for (String col : statusColumns) {
            if (!insertColumns.contains(col)) {
                missingStatusColumns.add(col);
            }
        }
        result.put("missingStatusColumns", missingStatusColumns);

        result.put("sampleRows", sampleRows);
        return result;
    }

    private long countInsertStatements(Path dumpPath, String tableName) throws Exception {
        long count = 0L;
        String insertPrefix = "INSERT INTO `" + tableName + "`";
        try (BufferedReader reader = Files.newBufferedReader(dumpPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(insertPrefix)) {
                    count++;
                }
            }
        }
        return count;
    }

    @FunctionalInterface
    private interface InsertRowConsumer {
        void accept(List<String> columns, List<String> values) throws Exception;
    }

    private void forEachInsertRow(Path dumpPath, String tableName, InsertRowConsumer consumer)
            throws Exception {
        boolean inInsert = false;
        List<String> columns = null;
        String insertPrefix = "INSERT INTO `" + tableName + "`";

        try (BufferedReader reader = Files.newBufferedReader(dumpPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!inInsert) {
                    if (line.startsWith(insertPrefix)) {
                        columns = parseInsertColumns(line);
                        inInsert = true;
                    }
                    continue;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                boolean endsSection = trimmed.endsWith(";");
                String normalized = trimmed;
                if (endsSection) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                normalized = normalized.stripTrailing();

                if (!normalized.isEmpty()) {
                    if (normalized.endsWith(",")) {
                        normalized = normalized.substring(0, normalized.length() - 1);
                    }

                    for (String tuple : extractTuples(normalized)) {
                        List<String> values = parseSqlTupleValues(tuple);
                        consumer.accept(columns, values);
                    }
                }

                if (endsSection) {
                    inInsert = false;
                    columns = null;
                }
            }
        }
    }

    private List<String> parseInsertColumns(String insertHeaderLine) {
        int start = insertHeaderLine.indexOf('(');
        int end = insertHeaderLine.indexOf(')', start + 1);
        if (start < 0 || end < 0 || end <= start) {
            return List.of();
        }

        String inside = insertHeaderLine.substring(start + 1, end);
        String[] parts = inside.split(",");
        List<String> cols = new ArrayList<>(parts.length);
        for (String part : parts) {
            String c = part.trim();
            if (c.startsWith("`") && c.endsWith("`") && c.length() >= 2) {
                c = c.substring(1, c.length() - 1);
            }
            cols.add(c);
        }
        return cols;
    }

    private List<String> extractTuples(String valuesLine) {
        List<String> tuples = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int tupleStart = -1;

        for (int i = 0; i < valuesLine.length(); i++) {
            char ch = valuesLine.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }

            if (inString) {
                if (ch == '\\') {
                    escaped = true;
                } else if (ch == '\'') {
                    inString = false;
                }
                continue;
            }

            if (ch == '\'') {
                inString = true;
                continue;
            }

            if (ch == '(') {
                if (depth == 0) {
                    tupleStart = i;
                }
                depth++;
                continue;
            }

            if (ch == ')') {
                depth--;
                if (depth == 0 && tupleStart >= 0) {
                    tuples.add(valuesLine.substring(tupleStart, i + 1));
                    tupleStart = -1;
                }
            }
        }

        return tuples;
    }

    private List<String> parseSqlTupleValues(String tuple) {
        String trimmed = tuple.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }

            if (inString) {
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '\'') {
                    inString = false;
                    continue;
                }
                current.append(ch);
                continue;
            }

            if (ch == '\'') {
                inString = true;
                continue;
            }

            if (ch == ',') {
                values.add(normalizeSqlValue(current.toString().trim()));
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        values.add(normalizeSqlValue(current.toString().trim()));
        return values;
    }

    private String normalizeSqlValue(String token) {
        if (token.equalsIgnoreCase("NULL")) {
            return null;
        }
        return token;
    }

    private String getByColumn(List<String> columns, List<String> values, String column) {
        int idx = columns.indexOf(column);
        if (idx < 0 || idx >= values.size()) {
            return null;
        }
        return values.get(idx);
    }

    private Long asLong(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return Long.parseLong(token.trim());
    }

    private BigDecimal asBigDecimal(String token, BigDecimal defaultValue) {
        if (token == null || token.isBlank()) {
            return defaultValue;
        }
        return new BigDecimal(token.trim());
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String chooseItemCode(String sku, String barcode, Long sourceId) {
        String candidate = normalizeBlankToNull(sku);
        if (candidate != null) {
            return candidate;
        }
        candidate = normalizeBlankToNull(barcode);
        if (candidate != null) {
            return candidate;
        }
        return "NEXO-" + sourceId;
    }

    private Map<Long, String> resolveUomByGroupId(Path dumpPath) throws Exception {
        Map<Long, String> groupNames = new HashMap<>();
        Map<Long, String> uomByGroupId = new HashMap<>();

        forEachInsertRow(
                dumpPath,
                TABLE_UNIT_GROUPS,
                (columns, values) -> {
                    Long id = asLong(getByColumn(columns, values, "id"));
                    String name = normalizeBlankToNull(getByColumn(columns, values, "name"));
                    if (id != null && name != null) {
                        groupNames.put(id, name);
                    }
                });

        forEachInsertRow(
                dumpPath,
                TABLE_UNITS,
                (columns, values) -> {
                    Long groupId = asLong(getByColumn(columns, values, "group_id"));
                    String identifier = normalizeBlankToNull(getByColumn(columns, values, "identifier"));
                    String name = normalizeBlankToNull(getByColumn(columns, values, "name"));
                    Long baseUnit = asLong(getByColumn(columns, values, "base_unit"));

                    if (groupId == null) {
                        return;
                    }

                    String candidate = identifier != null ? identifier : name;
                    if (candidate == null) {
                        return;
                    }

                    if (baseUnit != null && baseUnit == 1L) {
                        String existing = uomByGroupId.get(groupId);
                        uomByGroupId.put(groupId, chooseBetterUom(existing, candidate));
                    }
                });

        for (Map.Entry<Long, String> e : groupNames.entrySet()) {
            uomByGroupId.putIfAbsent(e.getKey(), e.getValue());
        }

        return uomByGroupId;
    }

    private String chooseBetterUom(String existing, String candidate) {
        if (existing == null) {
            return candidate;
        }
        if (existing.equalsIgnoreCase("pcs")) {
            return existing;
        }
        if (candidate.equalsIgnoreCase("pcs")) {
            return candidate;
        }
        return existing;
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
