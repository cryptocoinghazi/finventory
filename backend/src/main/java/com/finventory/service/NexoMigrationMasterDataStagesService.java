package com.finventory.service;

import com.finventory.dto.ItemDto;
import com.finventory.dto.PartyDto;
import com.finventory.dto.StockAdjustmentDto;
import com.finventory.dto.TaxSlabDto;
import com.finventory.dto.WarehouseDto;
import com.finventory.model.Item;
import com.finventory.model.MigrationIdMap;
import com.finventory.model.MigrationLogEntry;
import com.finventory.model.MigrationLogLevel;
import com.finventory.model.MigrationRun;
import com.finventory.model.MigrationStageKey;
import com.finventory.model.Party;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.MigrationIdMapRepository;
import com.finventory.repository.MigrationLogEntryRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.TaxSlabRepository;
import com.finventory.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NexoMigrationMasterDataStagesService {
    private static final String ENTITY_TYPE_ITEM = "ITEM";
    private static final String ENTITY_TYPE_TAX_SLAB = "TAX_SLAB";
    private static final String ENTITY_TYPE_WAREHOUSE = "WAREHOUSE";
    private static final String ENTITY_TYPE_PARTY_CUSTOMER = "PARTY_CUSTOMER";
    private static final String ENTITY_TYPE_PARTY_VENDOR = "PARTY_VENDOR";
    private static final String ENTITY_TYPE_STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT";

    private static final String TABLE_PRODUCTS = "ns_nexopos_products";
    private static final String TABLE_UNITS = "ns_nexopos_units";
    private static final String TABLE_UNIT_GROUPS = "ns_nexopos_units_groups";
    private static final String TABLE_CUSTOMERS = "ns_nexopos_customers";
    private static final String TABLE_PROVIDERS = "ns_nexopos_providers";
    private static final String TABLE_TAXES = "ns_nexopos_taxes";

    private static final int ANALYZE_SAMPLE_ROWS = 3;
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.ZERO;
    private static final String DEFAULT_UOM = "pcs";
    private static final int MAX_ERROR_SAMPLES = 25;
    private static final int MAX_MISSING_UNIT_GROUP_SAMPLES = 25;

    private final NexoDumpSqlService dumpSql;
    private final MigrationIdMapRepository idMapRepository;
    private final MigrationLogEntryRepository logEntryRepository;
    private final ItemRepository itemRepository;
    private final TaxSlabRepository taxSlabRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartyRepository partyRepository;
    private final ItemService itemService;
    private final TaxSlabService taxSlabService;
    private final WarehouseService warehouseService;
    private final PartyService partyService;
    private final StockAdjustmentService stockAdjustmentService;

    public Map<String, Object> analyzeSource(Path dumpPath) throws Exception {
        ensureDumpExists(dumpPath);

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
                TABLE_PRODUCTS,
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
                analyzeTable(dumpPath, TABLE_TAXES, List.of("name", "rate", "tax_group_id"), List.of()));

        stats.put(
                TABLE_UNITS,
                analyzeTable(
                        dumpPath,
                        TABLE_UNITS,
                        List.of("name", "identifier", "group_id", "base_unit"),
                        List.of()));

        return stats;
    }

    public Map<String, Object> importUnits(MigrationRun run, Path dumpPath) throws Exception {
        ensureDumpExists(dumpPath);

        Map<Long, String> uomByGroupId = resolveUomByGroupId(dumpPath);

        ImportUnitsCounters counters = new ImportUnitsCounters();
        List<Long> missingUnitGroupMappingSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();
        Map<String, AtomicLong> uomCounts = new LinkedHashMap<>();

        applyUomFromProductsToItems(
                run, dumpPath, uomByGroupId, counters, uomCounts, missingUnitGroupMappingSamples, errorSamples);

        return buildImportUnitsStats(
                run, dumpPath, uomByGroupId, counters, uomCounts, missingUnitGroupMappingSamples, errorSamples);
    }

    public Map<String, Object> importTaxSlabs(MigrationRun run, Path dumpPath) throws Exception {
        ensureDumpExists(dumpPath);

        ImportTaxSlabsCounters counters = new ImportTaxSlabsCounters();
        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();

        dumpSql.forEachInsertRow(
                dumpPath,
                TABLE_TAXES,
                (columns, values) -> importTaxSlabRow(run, counters, warningSamples, errorSamples, columns, values));

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
        stats.put("insertStatements", dumpSql.countInsertStatements(dumpPath, TABLE_TAXES));

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
        stats.put(
                "gstHandling",
                "Finventory stores total GST rate; invoices split CGST/SGST/IGST based on state codes.");

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

    public Map<String, Object> importWarehouses(MigrationRun run, Path dumpPath) throws Exception {
        ensureDumpExists(dumpPath);

        List<String> candidateTables =
                List.of(
                        "ns_nexopos_stores",
                        "ns_nexopos_warehouses",
                        "ns_nexopos_locations",
                        "ns_nexopos_branches");

        String selectedTable = selectFirstTableWithInserts(dumpPath, candidateTables);

        ImportWarehousesCounters counters = new ImportWarehousesCounters();
        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();

        if (selectedTable == null) {
            return buildNoWarehouseSourceStats(run, dumpPath, counters, warningSamples, errorSamples);
        }

        String tableName = selectedTable;
        dumpSql.forEachInsertRow(
                dumpPath,
                tableName,
                (columns, values) ->
                        importWarehouseRow(
                                run, tableName, counters, warningSamples, errorSamples, columns, values));

        return buildWarehouseStats(run, dumpPath, tableName, counters, warningSamples, errorSamples);
    }

    public Map<String, Object> importParties(MigrationRun run, Path dumpPath) throws Exception {
        ensureDumpExists(dumpPath);

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

        result.put(
                "customers",
                importPartyTable(run, dumpPath, TABLE_CUSTOMERS, ENTITY_TYPE_PARTY_CUSTOMER, Party.PartyType.CUSTOMER));
        result.put(
                "vendors",
                importPartyTable(run, dumpPath, TABLE_PROVIDERS, ENTITY_TYPE_PARTY_VENDOR, Party.PartyType.VENDOR));
        return result;
    }

    public Map<String, Object> importItems(MigrationRun run, Path dumpPath) throws Exception {
        ensureDumpExists(dumpPath);

        Map<Long, String> uomByGroupId = resolveUomByGroupId(dumpPath);
        Map<Long, BigDecimal> taxRateByTaxId = resolveTaxRateByTaxId(dumpPath);
        Map<Long, BigDecimal> taxRateByTaxGroupId = resolveTaxRateByTaxGroupId(dumpPath);
        Map<Long, Long> vendorSourceIdByProductId = resolveVendorSourceIdByProductId(dumpPath);

        ImportItemsCounters counters = new ImportItemsCounters();
        List<String> errorSamples = new ArrayList<>();
        List<String> warningSamples = new ArrayList<>();
        ItemImportSamples samples = new ItemImportSamples(warningSamples, errorSamples);
        ItemRowContext rowContext =
                new ItemRowContext(
                        run,
                        uomByGroupId,
                        taxRateByTaxId,
                        taxRateByTaxGroupId,
                        vendorSourceIdByProductId,
                        counters,
                        samples);

        dumpSql.forEachInsertRow(
                dumpPath,
                TABLE_PRODUCTS,
                (columns, values) -> importItemRow(rowContext, columns, values));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("implemented", true);
        stats.put("stage", MigrationStageKey.IMPORT_ITEMS.name());
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", run.isDryRun());
        stats.put("sourceSystem", run.getSourceSystem());
        stats.put("sourceReference", run.getSourceReference());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("insertStatements", dumpSql.countInsertStatements(dumpPath, TABLE_PRODUCTS));
        stats.put("found", counters.found.get());
        stats.put("valid", counters.valid.get());
        stats.put("skippedOutOfScope", counters.skippedOutOfScope.get());
        stats.put("skippedOverLimit", counters.skippedOverLimit.get());
        stats.put("alreadyMapped", counters.alreadyMapped.get());
        stats.put("linkedExisting", counters.linkedExisting.get());
        stats.put("created", counters.created.get());
        stats.put("updated", counters.updated.get());
        stats.put("wouldCreate", counters.wouldCreate.get());
        stats.put("wouldUpdate", counters.wouldUpdate.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
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

    public Map<String, Object> importOpeningStock(MigrationRun run, Path dumpPath) throws Exception {
        ensureDumpExists(dumpPath);

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

        OpeningStockCounters counters = new OpeningStockCounters();
        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();

        OpeningStockContext ctx = new OpeningStockContext(run, warehouseId, counters, warningSamples, errorSamples);

        dumpSql.forEachInsertRow(
                dumpPath,
                TABLE_PRODUCTS,
                (columns, values) -> importOpeningStockRow(ctx, columns, values));

        Map<String, Object> stats =
                buildOpeningStockStats(
                        run, dumpPath, warehouseId, counters, warningSamples, errorSamples);

        log(
                run,
                MigrationStageKey.IMPORT_OPENING_STOCK,
                MigrationLogLevel.INFO,
                "Opening stock import finished",
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

    private void importOpeningStockRow(OpeningStockContext ctx, List<String> columns, List<String> values) {
        Long productId = asLong(getByColumn(columns, values, "id"));
        if (productId == null) {
            return;
        }

        ctx.counters.found.incrementAndGet();

        if (!isInScope(ctx.run, productId)) {
            ctx.counters.skippedOutOfScope.incrementAndGet();
            return;
        }

        if (isOverLimit(ctx.run, ctx.counters.inScope)) {
            ctx.counters.skippedOverLimit.incrementAndGet();
            return;
        }
        ctx.counters.inScope.incrementAndGet();

        Optional<MigrationIdMap> existingMap =
                idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                        ctx.run.getSourceSystem(), ENTITY_TYPE_STOCK_ADJUSTMENT, productId);
        if (existingMap.isPresent()) {
            ctx.counters.alreadyMapped.incrementAndGet();
            return;
        }

        Optional<MigrationIdMap> itemMap =
                idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                        ctx.run.getSourceSystem(), ENTITY_TYPE_ITEM, productId);
        if (itemMap.isEmpty()) {
            ctx.counters.skippedMissingItemMap.incrementAndGet();
            addSample(ctx.warningSamples, "productId=" + productId + " missing item mapping");
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
            ctx.counters.skippedMissingQuantity.incrementAndGet();
            return;
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            ctx.counters.skippedNonPositiveQuantity.incrementAndGet();
            return;
        }

        if (ctx.run.isDryRun()) {
            ctx.counters.wouldCreate.incrementAndGet();
            return;
        }

        try {
            StockAdjustmentDto dto =
                    StockAdjustmentDto.builder()
                            .adjustmentDate(LocalDate.now())
                            .warehouseId(ctx.warehouseId)
                            .itemId(itemMap.get().getTargetId())
                            .quantity(quantity)
                            .reason("Opening stock import (NexoPOS productId=" + productId + ")")
                            .build();
            StockAdjustmentDto createdAdj = stockAdjustmentService.createAdjustment(dto);
            ctx.counters.created.incrementAndGet();
            saveIdMap(
                    ctx.run.getSourceSystem(),
                    ENTITY_TYPE_STOCK_ADJUSTMENT,
                    productId,
                    createdAdj.getId());
        } catch (Exception e) {
            ctx.counters.errors.incrementAndGet();
            addSample(ctx.errorSamples, "productId=" + productId + ": " + e.getMessage());
        }
    }

    private Map<String, Object> buildOpeningStockStats(
            MigrationRun run,
            Path dumpPath,
            UUID warehouseId,
            OpeningStockCounters counters,
            List<String> warningSamples,
            List<String> errorSamples)
            throws Exception {
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
        stats.put("insertStatements", dumpSql.countInsertStatements(dumpPath, TABLE_PRODUCTS));
        stats.put("found", counters.found.get());
        stats.put("inScope", counters.inScope.get());
        stats.put("skippedOutOfScope", counters.skippedOutOfScope.get());
        stats.put("skippedOverLimit", counters.skippedOverLimit.get());
        stats.put("alreadyMapped", counters.alreadyMapped.get());
        stats.put("skippedMissingItemMap", counters.skippedMissingItemMap.get());
        stats.put("skippedMissingQuantity", counters.skippedMissingQuantity.get());
        stats.put("skippedNonPositiveQuantity", counters.skippedNonPositiveQuantity.get());
        stats.put("created", counters.created.get());
        stats.put("wouldCreate", counters.wouldCreate.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
        stats.put("errors", counters.errors.get());
        stats.put("errorSamples", errorSamples);
        return stats;
    }

    private UUID resolveDefaultWarehouseId(MigrationRun run) {
        List<com.finventory.model.Warehouse> warehouses = warehouseRepository.findAll().stream().limit(1).toList();
        if (!warehouses.isEmpty()) {
            return warehouses.get(0).getId();
        }

        if (run.isDryRun()) {
            return null;
        }

        WarehouseDto created = warehouseService.createWarehouse(WarehouseDto.builder().name("Main Warehouse").build());
        return created.getId();
    }

    private void ensureDumpExists(Path dumpPath) {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }
    }

    private Map<String, Object> analyzeTable(
            Path dumpPath, String tableName, List<String> importantColumns, List<String> statusColumns)
            throws Exception {
        long insertStatements = dumpSql.countInsertStatements(dumpPath, tableName);

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

        dumpSql.forEachInsertRow(
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

    private void importTaxSlabRow(
            MigrationRun run,
            ImportTaxSlabsCounters counters,
            List<String> warningSamples,
            List<String> errorSamples,
            List<String> columns,
            List<String> values) {
        Long sourceId = asLong(getByColumn(columns, values, "id"));
        if (sourceId == null) {
            counters.invalid.incrementAndGet();
            addSample(warningSamples, "taxId=<NULL>");
            return;
        }

        counters.found.incrementAndGet();

        if (!isInScope(run, sourceId)) {
            counters.skippedOutOfScope.incrementAndGet();
            return;
        }

        if (isOverLimit(run, counters.inScope)) {
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
                addSample(warningSamples, "taxId=" + sourceId + " missing rate");
                return;
            }

            String name = normalizeBlankToNull(getByColumn(columns, values, "name"));
            TaxSlabDto dto = TaxSlabDto.builder().rate(rate).description(name).build();

            var existingByRate = taxSlabRepository.findByRate(rate);
            if (existingByRate.isPresent()) {
                counters.linkedExisting.incrementAndGet();
                if (!run.isDryRun()) {
                    saveIdMap(run.getSourceSystem(), ENTITY_TYPE_TAX_SLAB, sourceId, existingByRate.get().getId());
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
            addSample(errorSamples, "taxId=" + sourceId + ": " + e.getMessage());
        }
    }

    private Map<String, Object> buildNoWarehouseSourceStats(
            MigrationRun run,
            Path dumpPath,
            ImportWarehousesCounters counters,
            List<String> warningSamples,
            List<String> errorSamples) {
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

        return stats;
    }

    private void importWarehouseRow(
            MigrationRun run,
            String selectedTable,
            ImportWarehousesCounters counters,
            List<String> warningSamples,
            List<String> errorSamples,
            List<String> columns,
            List<String> values) {
        Long sourceId = asLong(getByColumn(columns, values, "id"));
        if (sourceId == null) {
            counters.invalid.incrementAndGet();
            addSample(warningSamples, "warehouseId=<NULL>");
            return;
        }

        counters.found.incrementAndGet();

        if (!isInScope(run, sourceId)) {
            counters.skippedOutOfScope.incrementAndGet();
            return;
        }

        if (isOverLimit(run, counters.inScope)) {
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
                    normalizeBlankToNull(
                            firstNonBlank(columns, values, List.of("name", "title", "label", "description")));
            if (name == null) {
                name = "Warehouse-" + sourceId;
            }

            String location =
                    normalizeBlankToNull(
                            firstNonBlank(columns, values, List.of("location", "address", "city", "country")));
            String stateCode =
                    normalizeStateCode(firstNonBlank(columns, values, List.of("state_code", "state", "state_id")));

            Optional<com.finventory.model.Warehouse> existingByName = warehouseRepository.findByName(name);
            if (existingByName.isPresent()) {
                counters.linkedExisting.incrementAndGet();
                if (!run.isDryRun()) {
                    saveIdMap(run.getSourceSystem(), ENTITY_TYPE_WAREHOUSE, sourceId, existingByName.get().getId());
                }
                return;
            }

            WarehouseDto dto = WarehouseDto.builder().name(name).location(location).stateCode(stateCode).build();

            if (run.isDryRun()) {
                counters.wouldCreate.incrementAndGet();
                return;
            }

            WarehouseDto created = warehouseService.createWarehouse(dto);
            counters.created.incrementAndGet();
            saveIdMap(run.getSourceSystem(), ENTITY_TYPE_WAREHOUSE, sourceId, created.getId());
        } catch (Exception e) {
            counters.errors.incrementAndGet();
            addSample(errorSamples, "warehouseId=" + sourceId + ": " + e.getMessage());
        }
    }

    private Map<String, Object> buildWarehouseStats(
            MigrationRun run,
            Path dumpPath,
            String selectedTable,
            ImportWarehousesCounters counters,
            List<String> warningSamples,
            List<String> errorSamples)
            throws Exception {
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
        stats.put("insertStatements", dumpSql.countInsertStatements(dumpPath, selectedTable));

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

    private Map<String, Object> importPartyTable(
            MigrationRun run, Path dumpPath, String tableName, String entityType, Party.PartyType partyType)
            throws Exception {
        ImportPartiesCounters counters = new ImportPartiesCounters();
        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();
        PartyRowContext rowContext =
                new PartyRowContext(run, entityType, partyType, counters, warningSamples, errorSamples);

        dumpSql.forEachInsertRow(
                dumpPath,
                tableName,
                (columns, values) -> importPartyRow(rowContext, columns, values));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("table", tableName);
        stats.put("partyType", partyType.name());
        stats.put("insertStatements", dumpSql.countInsertStatements(dumpPath, tableName));
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

    private void importPartyRow(PartyRowContext ctx, List<String> columns, List<String> values) {
        Long sourceId = asLong(getByColumn(columns, values, "id"));
        if (sourceId == null) {
            ctx.counters.invalid.incrementAndGet();
            addSample(ctx.warningSamples, "partyId=<NULL>");
            return;
        }

        ctx.counters.found.incrementAndGet();

        if (!isInScope(ctx.run, sourceId)) {
            ctx.counters.skippedOutOfScope.incrementAndGet();
            return;
        }

        if (isOverLimit(ctx.run, ctx.counters.inScope)) {
            ctx.counters.skippedOverLimit.incrementAndGet();
            return;
        }
        ctx.counters.inScope.incrementAndGet();

        Optional<MigrationIdMap> existingMap =
                idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                        ctx.run.getSourceSystem(), ctx.entityType, sourceId);
        if (existingMap.isPresent()) {
            ctx.counters.alreadyMapped.incrementAndGet();
            return;
        }

        try {
            PartyDto dto = buildPartyDto(ctx.partyType, columns, values, sourceId);
            Optional<UUID> linked =
                    tryLinkExistingParty(
                            ctx.run,
                            ctx.entityType,
                            sourceId,
                            ctx.partyType,
                            dto,
                            ctx.warningSamples,
                            ctx.counters);
            if (linked.isPresent()) {
                return;
            }

            if (ctx.run.isDryRun()) {
                ctx.counters.wouldCreate.incrementAndGet();
                return;
            }

            PartyDto created = partyService.createParty(dto);
            ctx.counters.created.incrementAndGet();
            saveIdMap(ctx.run.getSourceSystem(), ctx.entityType, sourceId, created.getId());
        } catch (Exception e) {
            ctx.counters.errors.incrementAndGet();
            addSample(ctx.errorSamples, "partyId=" + sourceId + ": " + e.getMessage());
        }
    }

    private static final class PartyRowContext {
        private final MigrationRun run;
        private final String entityType;
        private final Party.PartyType partyType;
        private final ImportPartiesCounters counters;
        private final List<String> warningSamples;
        private final List<String> errorSamples;

        private PartyRowContext(
                MigrationRun run,
                String entityType,
                Party.PartyType partyType,
                ImportPartiesCounters counters,
                List<String> warningSamples,
                List<String> errorSamples) {
            this.run = run;
            this.entityType = entityType;
            this.partyType = partyType;
            this.counters = counters;
            this.warningSamples = warningSamples;
            this.errorSamples = errorSamples;
        }
    }

    private PartyDto buildPartyDto(
            Party.PartyType partyType, List<String> columns, List<String> values, Long sourceId) {
        String firstName = normalizeBlankToNull(getByColumn(columns, values, "first_name"));
        String lastName = normalizeBlankToNull(getByColumn(columns, values, "last_name"));
        String fullName = normalizeBlankToNull(getByColumn(columns, values, "name"));
        String name =
                fullName != null
                        ? fullName
                        : normalizeBlankToNull(
                                (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName));
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
        String phone = normalizeBlankToNull(firstNonBlank(columns, values, List.of("phone", "mobile")));
        String address =
                normalizeBlankToNull(
                        firstNonBlank(
                                columns,
                                values,
                                List.of("address", "billing_address", "shipping_address")));

        String stateCode =
                normalizeStateCode(firstNonBlank(columns, values, List.of("state_code", "state", "state_id")));
        if (stateCode == null && gstin != null && gstin.length() >= 2) {
            stateCode = normalizeStateCode(gstin.substring(0, 2));
        }

        return PartyDto.builder()
                .name(name)
                .type(partyType)
                .gstin(gstin)
                .stateCode(stateCode)
                .address(address)
                .phone(phone)
                .email(email)
                .build();
    }

    private Optional<UUID> tryLinkExistingParty(
            MigrationRun run,
            String entityType,
            Long sourceId,
            Party.PartyType partyType,
            PartyDto dto,
            List<String> warningSamples,
            ImportPartiesCounters counters) {
        String gstin = dto.getGstin();
        if (gstin != null) {
            Optional<Party> existingByGstin = partyRepository.findByGstin(gstin);
            if (existingByGstin.isPresent()) {
                counters.linkedExisting.incrementAndGet();
                if (!run.isDryRun()) {
                    saveIdMap(run.getSourceSystem(), entityType, sourceId, existingByGstin.get().getId());
                }
                return Optional.of(existingByGstin.get().getId());
            }
        }

        List<Party> existingByName = partyRepository.findByNameIgnoreCaseAndType(dto.getName(), partyType);
        if (existingByName.size() == 1) {
            counters.linkedExisting.incrementAndGet();
            if (!run.isDryRun()) {
                saveIdMap(run.getSourceSystem(), entityType, sourceId, existingByName.get(0).getId());
            }
            return Optional.of(existingByName.get(0).getId());
        }
        if (existingByName.size() > 1) {
            counters.ambiguousExisting.incrementAndGet();
            addSample(warningSamples, "partyId=" + sourceId + " ambiguous existing matches for name=" + dto.getName());
            return Optional.of(existingByName.get(0).getId());
        }

        return Optional.empty();
    }

    private void importItemRow(ItemRowContext ctx, List<String> columns, List<String> values) {
        Long sourceId = asLong(getByColumn(columns, values, "id"));
        if (sourceId == null) {
            ctx.counters.errors.incrementAndGet();
            addSample(ctx.samples.errorSamples, "productId=<NULL>");
            return;
        }

        ctx.counters.found.incrementAndGet();

        if (!isInScope(ctx.run, sourceId)) {
            ctx.counters.skippedOutOfScope.incrementAndGet();
            return;
        }

        if (isOverLimit(ctx.run, ctx.counters.valid)) {
            ctx.counters.skippedOverLimit.incrementAndGet();
            return;
        }

        ctx.counters.valid.incrementAndGet();

        Optional<MigrationIdMap> existingMap =
                idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                        ctx.run.getSourceSystem(), ENTITY_TYPE_ITEM, sourceId);

        try {
            ItemUpsert upsert = parseItemUpsert(ctx, sourceId, columns, values);

            if (upsert == null) {
                return;
            }

            Item targetItem = null;
            if (existingMap.isPresent()) {
                ctx.counters.alreadyMapped.incrementAndGet();
                targetItem = itemRepository.findById(existingMap.get().getTargetId()).orElse(null);
            } else if (upsert.code != null) {
                Optional<Item> existingByCode = itemRepository.findByCode(upsert.code);
                if (existingByCode.isPresent()) {
                    ctx.counters.linkedExisting.incrementAndGet();
                    targetItem = existingByCode.get();
                    if (!ctx.run.isDryRun()) {
                        saveIdMap(ctx.run.getSourceSystem(), ENTITY_TYPE_ITEM, sourceId, targetItem.getId());
                    }
                }
            }

            if (targetItem != null) {
                boolean changed = applyItemUpdates(targetItem, upsert);
                if (!changed) {
                    return;
                }
                if (ctx.run.isDryRun()) {
                    ctx.counters.wouldUpdate.incrementAndGet();
                    return;
                }
                itemRepository.save(targetItem);
                ctx.counters.updated.incrementAndGet();
                if (existingMap.isEmpty()) {
                    saveIdMap(ctx.run.getSourceSystem(), ENTITY_TYPE_ITEM, sourceId, targetItem.getId());
                }
                return;
            }

            if (ctx.run.isDryRun()) {
                ctx.counters.wouldCreate.incrementAndGet();
                return;
            }

            ItemDto created = itemService.createItem(upsert.dto);
            ctx.counters.created.incrementAndGet();
            saveIdMap(ctx.run.getSourceSystem(), ENTITY_TYPE_ITEM, sourceId, created.getId());
        } catch (Exception e) {
            ctx.counters.errors.incrementAndGet();
            addSample(ctx.samples.errorSamples, "productId=" + sourceId + ": " + e.getMessage());
        }
    }

    private boolean applyItemUpdates(Item targetItem, ItemUpsert upsert) {
        boolean changed = false;

        if (upsert.vendorId != null
                && (targetItem.getVendor() == null || !upsert.vendorId.equals(targetItem.getVendor().getId()))) {
            Party vendor = partyRepository.findById(upsert.vendorId).orElse(null);
            if (vendor != null) {
                targetItem.setVendor(vendor);
                changed = true;
            }
        }

        if (upsert.uom != null && !upsert.uom.equals(targetItem.getUom())) {
            targetItem.setUom(upsert.uom);
            changed = true;
        }

        if (upsert.taxRate != null
                && targetItem.getTaxRate() != null
                && upsert.taxRate.compareTo(targetItem.getTaxRate()) != 0) {
            targetItem.setTaxRate(upsert.taxRate);
            changed = true;
        }

        if (upsert.unitPrice != null
                && targetItem.getUnitPrice() != null
                && upsert.unitPrice.compareTo(targetItem.getUnitPrice()) != 0) {
            targetItem.setUnitPrice(upsert.unitPrice);
            changed = true;
        }

        String desiredBarcode = normalizeBlankToNull(upsert.barcode);
        if (desiredBarcode != null
                && (targetItem.getBarcode() == null || targetItem.getBarcode().isBlank())
                && !itemRepository.existsByBarcode(desiredBarcode)) {
            targetItem.setBarcode(desiredBarcode);
            changed = true;
        }

        return changed;
    }

    private ItemUpsert parseItemUpsert(ItemRowContext ctx, Long sourceId, List<String> columns, List<String> values) {
        String name = normalizeBlankToNull(getByColumn(columns, values, "name"));
        if (name == null) {
            addSample(ctx.samples.warningSamples, "productId=" + sourceId + " missing name");
            return null;
        }

        String sku = normalizeBlankToNull(getByColumn(columns, values, "sku"));
        String barcode = normalizeBlankToNull(getByColumn(columns, values, "barcode"));
        Long unitGroupId = asLong(getByColumn(columns, values, "unit_group"));
        Long taxGroupId = asLong(getByColumn(columns, values, "tax_group_id"));
        Long taxId = asLong(getByColumn(columns, values, "tax_id"));
        BigDecimal taxRate =
                resolveTaxRate(columns, values, ctx.taxRateByTaxId, ctx.taxRateByTaxGroupId, taxGroupId, taxId);

        Long vendorSourceId =
                asLong(firstNonBlank(columns, values, List.of("provider_id", "vendor_id", "supplier_id")));
        if (vendorSourceId == null) {
            vendorSourceId = ctx.vendorSourceIdByProductId.get(sourceId);
        }

        UUID vendorId = null;
        if (vendorSourceId != null) {
            Optional<MigrationIdMap> vendorMap =
                    idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                            ctx.run.getSourceSystem(), ENTITY_TYPE_PARTY_VENDOR, vendorSourceId);
            if (vendorMap.isPresent()) {
                vendorId = vendorMap.get().getTargetId();
            } else {
                addSample(
                        ctx.samples.warningSamples,
                        "productId="
                                + sourceId
                                + " missing vendor mapping for vendorId="
                                + vendorSourceId);
            }
        }

        String uom =
                unitGroupId == null ? DEFAULT_UOM : ctx.uomByGroupId.getOrDefault(unitGroupId, DEFAULT_UOM);
        String code = chooseItemCode(sku, barcode, sourceId);

        BigDecimal unitPrice =
                asBigDecimal(
                        firstNonBlank(
                                columns,
                                values,
                                List.of("sale_price", "selling_price", "price")),
                        BigDecimal.ZERO);

        ItemDto dto =
                ItemDto.builder()
                        .name(name)
                        .code(code)
                        .barcode(
                                barcode != null && itemRepository.existsByBarcode(barcode) ? null : barcode)
                        .uom(uom)
                        .unitPrice(unitPrice)
                        .taxRate(taxRate)
                        .vendorId(vendorId)
                        .build();

        ItemUpsert upsert = new ItemUpsert();
        upsert.dto = dto;
        upsert.code = code;
        upsert.vendorId = vendorId;
        upsert.uom = uom;
        upsert.taxRate = taxRate;
        upsert.unitPrice = unitPrice;
        upsert.barcode = barcode;
        return upsert;
    }

    private BigDecimal resolveTaxRate(
            List<String> columns,
            List<String> values,
            Map<Long, BigDecimal> taxRateByTaxId,
            Map<Long, BigDecimal> taxRateByTaxGroupId,
            Long taxGroupId,
            Long taxId) {
        if (taxGroupId != null && taxRateByTaxGroupId.containsKey(taxGroupId)) {
            return taxRateByTaxGroupId.get(taxGroupId);
        }
        if (taxId != null && taxRateByTaxId.containsKey(taxId)) {
            return taxRateByTaxId.get(taxId);
        }
        return asBigDecimal(getByColumn(columns, values, "tax_value"), DEFAULT_TAX_RATE);
    }

    private static final class ItemUpsert {
        private ItemDto dto;
        private String code;
        private UUID vendorId;
        private String uom;
        private BigDecimal taxRate;
        private BigDecimal unitPrice;
        private String barcode;
    }

    private static final class ItemRowContext {
        private final MigrationRun run;
        private final Map<Long, String> uomByGroupId;
        private final Map<Long, BigDecimal> taxRateByTaxId;
        private final Map<Long, BigDecimal> taxRateByTaxGroupId;
        private final Map<Long, Long> vendorSourceIdByProductId;
        private final ImportItemsCounters counters;
        private final ItemImportSamples samples;

        private ItemRowContext(
                MigrationRun run,
                Map<Long, String> uomByGroupId,
                Map<Long, BigDecimal> taxRateByTaxId,
                Map<Long, BigDecimal> taxRateByTaxGroupId,
                Map<Long, Long> vendorSourceIdByProductId,
                ImportItemsCounters counters,
                ItemImportSamples samples) {
            this.run = run;
            this.uomByGroupId = uomByGroupId;
            this.taxRateByTaxId = taxRateByTaxId;
            this.taxRateByTaxGroupId = taxRateByTaxGroupId;
            this.vendorSourceIdByProductId = vendorSourceIdByProductId;
            this.counters = counters;
            this.samples = samples;
        }
    }

    private static final class ItemImportSamples {
        private final List<String> warningSamples;
        private final List<String> errorSamples;

        private ItemImportSamples(List<String> warningSamples, List<String> errorSamples) {
            this.warningSamples = warningSamples;
            this.errorSamples = errorSamples;
        }
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

    private Map<Long, String> resolveUomByGroupId(Path dumpPath) throws Exception {
        Map<Long, String> groupNames = new HashMap<>();
        Map<Long, String> uomByGroupId = new HashMap<>();

        dumpSql.forEachInsertRow(
                dumpPath,
                TABLE_UNIT_GROUPS,
                (columns, values) -> {
                    Long id = asLong(getByColumn(columns, values, "id"));
                    String name = normalizeBlankToNull(getByColumn(columns, values, "name"));
                    if (id != null && name != null) {
                        groupNames.put(id, name);
                    }
                });

        dumpSql.forEachInsertRow(
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
        if (existing.equalsIgnoreCase(DEFAULT_UOM)) {
            return existing;
        }
        if (candidate.equalsIgnoreCase(DEFAULT_UOM)) {
            return candidate;
        }
        return existing;
    }

    private Map<Long, BigDecimal> resolveTaxRateByTaxId(Path dumpPath) throws Exception {
        Map<Long, BigDecimal> rateById = new HashMap<>();
        dumpSql.forEachInsertRow(
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
        dumpSql.forEachInsertRow(
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
        String procurements = "ns_nexopos_procurements";
        String procurementsProducts = "ns_nexopos_procurements_products";

        long p = dumpSql.countInsertStatements(dumpPath, procurements);
        long pp = dumpSql.countInsertStatements(dumpPath, procurementsProducts);
        if (p == 0 || pp == 0) {
            return Map.of();
        }

        Map<Long, Long> providerIdByProcurementId = new HashMap<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                procurements,
                (columns, values) -> {
                    Long procurementId = asLong(getByColumn(columns, values, "id"));
                    Long providerId = asLong(getByColumn(columns, values, "provider_id"));
                    if (procurementId != null && providerId != null) {
                        providerIdByProcurementId.put(procurementId, providerId);
                    }
                });

        Map<Long, Map<Long, AtomicLong>> providerCountsByProductId = new HashMap<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                procurementsProducts,
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

    private void applyUomFromProductsToItems(
            MigrationRun run,
            Path dumpPath,
            Map<Long, String> uomByGroupId,
            ImportUnitsCounters counters,
            Map<String, AtomicLong> uomCounts,
            List<Long> missingUnitGroupMappingSamples,
            List<String> errorSamples)
            throws Exception {
        dumpSql.forEachInsertRow(
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

                    if (isOverLimit(run, counters.inScopeProducts)) {
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

                    if (run.isDryRun()) {
                        counters.wouldUpdate.incrementAndGet();
                        return;
                    }

                    try {
                        ItemDto existing = itemService.getItemById(existingMap.get().getTargetId());
                        if (existing.getUom() != null && existing.getUom().equals(uom)) {
                            counters.unchanged.incrementAndGet();
                            return;
                        }

                        ItemDto updated =
                                ItemDto.builder()
                                        .id(existing.getId())
                                        .name(existing.getName())
                                        .code(existing.getCode())
                                        .barcode(existing.getBarcode())
                                        .uom(uom)
                                        .unitPrice(existing.getUnitPrice())
                                        .taxRate(existing.getTaxRate())
                                        .vendorId(existing.getVendorId())
                                        .build();
                        itemService.updateItem(existing.getId(), updated);
                        counters.updated.incrementAndGet();
                    } catch (Exception e) {
                        counters.errors.incrementAndGet();
                        addSample(errorSamples, "productId=" + sourceId + ": " + e.getMessage());
                    }
                });
    }

    private Map<String, Object> buildImportUnitsStats(
            MigrationRun run,
            Path dumpPath,
            Map<Long, String> uomByGroupId,
            ImportUnitsCounters counters,
            Map<String, AtomicLong> uomCounts,
            List<Long> missingUnitGroupMappingSamples,
            List<String> errorSamples)
            throws Exception {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("implemented", true);
        stats.put("stage", MigrationStageKey.IMPORT_UNITS.name());
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", run.isDryRun());
        stats.put("sourceSystem", run.getSourceSystem());
        stats.put("sourceReference", run.getSourceReference());
        stats.put("scopeSourceIdMin", run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", run.getScopeSourceIdMax());
        stats.put("scopeLimit", run.getScopeLimit());
        stats.put("insertStatements", dumpSql.countInsertStatements(dumpPath, TABLE_PRODUCTS));

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
        stats.put("uomGroupMappings", uomByGroupId.size());

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

    private String selectFirstTableWithInserts(Path dumpPath, List<String> candidates) throws Exception {
        for (String t : candidates) {
            if (dumpSql.countInsertStatements(dumpPath, t) > 0) {
                return t;
            }
        }
        return null;
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

    private boolean isOverLimit(MigrationRun run, AtomicLong inScopeCounter) {
        Integer limit = run.getScopeLimit();
        return limit != null && inScopeCounter.get() >= limit;
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

    private String getByColumn(List<String> columns, List<String> values, String column) {
        return dumpSql.getByColumn(columns, values, column);
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

    private void addSample(List<String> samples, String value) {
        if (samples.size() < MAX_ERROR_SAMPLES) {
            samples.add(value);
        }
    }

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

    private static final class OpeningStockCounters {
        private final AtomicLong found = new AtomicLong();
        private final AtomicLong inScope = new AtomicLong();
        private final AtomicLong skippedOutOfScope = new AtomicLong();
        private final AtomicLong skippedOverLimit = new AtomicLong();
        private final AtomicLong skippedMissingItemMap = new AtomicLong();
        private final AtomicLong skippedMissingQuantity = new AtomicLong();
        private final AtomicLong skippedNonPositiveQuantity = new AtomicLong();
        private final AtomicLong alreadyMapped = new AtomicLong();
        private final AtomicLong created = new AtomicLong();
        private final AtomicLong wouldCreate = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
    }

    private static final class OpeningStockContext {
        private final MigrationRun run;
        private final UUID warehouseId;
        private final OpeningStockCounters counters;
        private final List<String> warningSamples;
        private final List<String> errorSamples;

        private OpeningStockContext(
                MigrationRun run,
                UUID warehouseId,
                OpeningStockCounters counters,
                List<String> warningSamples,
                List<String> errorSamples) {
            this.run = run;
            this.warehouseId = warehouseId;
            this.counters = counters;
            this.warningSamples = warningSamples;
            this.errorSamples = errorSamples;
        }
    }
}
