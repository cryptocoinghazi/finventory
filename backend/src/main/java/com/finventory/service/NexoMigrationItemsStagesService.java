package com.finventory.service;

import com.finventory.dto.ItemDto;
import com.finventory.dto.StockAdjustmentDto;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NexoMigrationItemsStagesService {
    private static final String ENTITY_TYPE_ITEM = "ITEM";
    private static final String ENTITY_TYPE_PARTY_VENDOR = "PARTY_VENDOR";
    private static final String ENTITY_TYPE_STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT";

    private static final String TABLE_PRODUCTS = "ns_nexopos_products";
    private static final String TABLE_PRODUCTS_UNIT_QUANTITIES =
            "ns_nexopos_products_unit_quantities";
    private static final String TABLE_TAXES = "ns_nexopos_taxes";
    private static final String TABLE_UNIT_GROUPS = "ns_nexopos_units_groups";
    private static final String TABLE_UNITS = "ns_nexopos_units";
    private static final String TABLE_ORDERS_PRODUCTS = "ns_nexopos_orders_products";
    private static final String TABLE_PROCUREMENTS_PRODUCTS = "ns_nexopos_procurements_products";

    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.ZERO;
    private static final String DEFAULT_UOM = "pcs";
    private static final int MAX_ERROR_SAMPLES = 25;
    private static final int MAX_UNSUPPORTED_SAMPLES = 10;

    private final NexoDumpSqlService dumpSql;
    private final MigrationIdMapRepository idMapRepository;
    private final MigrationLogEntryRepository logEntryRepository;
    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemService itemService;
    private final WarehouseService warehouseService;
    private final StockAdjustmentService stockAdjustmentService;

    public Map<String, Object> importItems(MigrationRun run, Path dumpPath) throws Exception {
        ensureDumpExists(dumpPath);

        Map<Long, String> uomByGroupId = resolveUomByGroupId(dumpPath);
        Map<Long, BigDecimal> taxRateByTaxId = resolveTaxRateByTaxId(dumpPath);
        Map<Long, BigDecimal> taxRateByTaxGroupId = resolveTaxRateByTaxGroupId(dumpPath);

        Map<Long, BigDecimal> avgOrderPriceByProductId =
                resolveAverageOrderPriceByProductId(dumpPath);
        Map<Long, BigDecimal> avgPurchasePriceByProductId =
                resolveAveragePurchasePriceByProductId(dumpPath);

        ItemImportLookups lookups =
                new ItemImportLookups(
                        uomByGroupId,
                        taxRateByTaxId,
                        taxRateByTaxGroupId,
                        avgOrderPriceByProductId,
                        avgPurchasePriceByProductId);

        ImportItemsCounters counters = new ImportItemsCounters();
        List<String> errorSamples = new ArrayList<>();
        List<String> warningSamples = new ArrayList<>();
        List<String> unsupportedFieldSamples = new ArrayList<>();
        ItemImportSamples samples = new ItemImportSamples(warningSamples, errorSamples);
        ItemRowContext rowContext =
                new ItemRowContext(run, lookups, counters, samples, unsupportedFieldSamples);

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
        stats.put("itemsCreated", counters.created.get());
        stats.put("itemsUpdated", counters.updated.get());
        stats.put("itemsWouldCreate", counters.wouldCreate.get());
        stats.put("itemsWouldUpdate", counters.wouldUpdate.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
        stats.put("errors", counters.errors.get());
        stats.put("errorSamples", errorSamples);

        Map<String, Object> priceSource = new LinkedHashMap<>();
        priceSource.put("fromProductColumns", rowContext.priceFromProductColumns.get());
        priceSource.put("fromOrders", rowContext.priceFromOrders.get());
        priceSource.put("defaultedZero", rowContext.priceDefaultedZero.get());
        stats.put("priceSource", priceSource);

        Map<String, Object> cogsSource = new LinkedHashMap<>();
        cogsSource.put("fromProductColumns", rowContext.cogsFromProductColumns.get());
        cogsSource.put("fromProcurements", rowContext.cogsFromProcurements.get());
        cogsSource.put("defaultedZero", rowContext.cogsDefaultedZero.get());
        stats.put("cogsSource", cogsSource);

        Map<String, Object> vendorLinkage = new LinkedHashMap<>();
        vendorLinkage.put(
                "productsWithCategoryId", rowContext.vendorLinkageProductsWithCategory.get());
        vendorLinkage.put("vendorMapped", rowContext.vendorLinkageMapped.get());
        vendorLinkage.put("vendorMissingMap", rowContext.vendorLinkageMissingMap.get());
        vendorLinkage.put("missingCategoryId", rowContext.vendorLinkageMissingCategory.get());
        stats.put("vendorLinkage", vendorLinkage);
        stats.put("vendorMapped", rowContext.vendorLinkageMapped.get());
        stats.put("vendorMissingMap", rowContext.vendorLinkageMissingMap.get());

        Map<String, Object> unsupported = new LinkedHashMap<>();
        unsupported.put("descriptionNonEmpty", rowContext.unsupportedDescriptionNonEmpty.get());
        unsupported.put("statusPresent", rowContext.unsupportedStatusPresent.get());
        unsupported.put("activePresent", rowContext.unsupportedStatusPresent.get());
        unsupported.put("cogsCandidatesPresent", rowContext.unsupportedCogsCandidatesPresent.get());
        unsupported.put("samples", unsupportedFieldSamples);
        stats.put("unsupportedSourceFields", unsupported);
        stats.put(
                "unsupportedDescriptionNonEmpty", rowContext.unsupportedDescriptionNonEmpty.get());
        stats.put("unsupportedStatusPresent", rowContext.unsupportedStatusPresent.get());
        stats.put("unsupportedActivePresent", rowContext.unsupportedStatusPresent.get());
        stats.put(
                "unsupportedCogsCandidatesPresent",
                rowContext.unsupportedCogsCandidatesPresent.get());

        log(
                run,
                MigrationStageKey.IMPORT_ITEMS,
                MigrationLogLevel.INFO,
                "Items import finished",
                "found="
                        + counters.found.get()
                        + ", created="
                        + counters.created.get()
                        + ", updated="
                        + counters.updated.get()
                        + ", vendorMapped="
                        + rowContext.vendorLinkageMapped.get()
                        + ", vendorMissingMap="
                        + rowContext.vendorLinkageMissingMap.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit());

        if (rowContext.unsupportedDescriptionNonEmpty.get() > 0
                || rowContext.unsupportedStatusPresent.get() > 0
                || rowContext.unsupportedCogsCandidatesPresent.get() > 0) {
            log(
                    run,
                    MigrationStageKey.IMPORT_ITEMS,
                    MigrationLogLevel.WARN,
                    "Source has fields not stored in Finventory Item",
                    "descriptionNonEmpty="
                            + rowContext.unsupportedDescriptionNonEmpty.get()
                            + ", activePresent="
                            + rowContext.unsupportedStatusPresent.get()
                            + ", cogsCandidatesPresent="
                            + rowContext.unsupportedCogsCandidatesPresent.get());
        }

        return stats;
    }

    public Map<String, Object> importOpeningStock(MigrationRun run, Path dumpPath)
            throws Exception {
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
        OpeningStockContext ctx =
                new OpeningStockContext(run, warehouseId, counters, warningSamples, errorSamples);

        java.util.HashSet<Long> seenProductIds = new java.util.HashSet<>();
        java.util.LinkedHashSet<Long> eligibleProductIds = new java.util.LinkedHashSet<>();
        Map<Long, BigDecimal> quantityByProductId = new HashMap<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                TABLE_PRODUCTS_UNIT_QUANTITIES,
                (columns, values) -> {
                    Long productId = asLong(getByColumn(columns, values, "product_id"));
                    if (productId == null) {
                        return;
                    }

                    boolean firstSeen = seenProductIds.add(productId);
                    if (firstSeen) {
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
                        eligibleProductIds.add(productId);
                    }

                    if (!eligibleProductIds.contains(productId)) {
                        return;
                    }

                    BigDecimal quantity =
                            asBigDecimal(getByColumn(columns, values, "quantity"), null);
                    if (quantity == null) {
                        return;
                    }
                    quantityByProductId.merge(productId, quantity, BigDecimal::add);
                });

        for (Long productId : eligibleProductIds) {
            importOpeningStockByProductId(ctx, productId, quantityByProductId.get(productId));
        }

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

    private void importOpeningStockByProductId(
            OpeningStockContext ctx, Long productId, BigDecimal quantity) {
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
        stats.put(
                "insertStatements",
                dumpSql.countInsertStatements(dumpPath, TABLE_PRODUCTS_UNIT_QUANTITIES));
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
        stats.put("stockAdjustmentsCreated", counters.created.get());
        stats.put("stockAdjustmentsWouldCreate", counters.wouldCreate.get());
        stats.put("warnings", warningSamples.size());
        stats.put("warningSamples", warningSamples);
        stats.put("errors", counters.errors.get());
        stats.put("errorSamples", errorSamples);
        return stats;
    }

    private UUID resolveDefaultWarehouseId(MigrationRun run) {
        Optional<com.finventory.model.Warehouse> main =
                warehouseRepository.findByName("Main Warehouse");
        if (main.isPresent()) {
            return main.get().getId();
        }

        List<com.finventory.model.Warehouse> warehouses =
                warehouseRepository
                        .findAll(PageRequest.of(0, 1, Sort.by("name").ascending()))
                        .getContent();
        if (!warehouses.isEmpty()) {
            return warehouses.get(0).getId();
        }

        if (run.isDryRun()) {
            return null;
        }

        WarehouseDto created =
                warehouseService.createWarehouse(
                        WarehouseDto.builder().name("Main Warehouse").build());
        return created.getId();
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
                        saveIdMap(
                                ctx.run.getSourceSystem(),
                                ENTITY_TYPE_ITEM,
                                sourceId,
                                targetItem.getId());
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
                    saveIdMap(
                            ctx.run.getSourceSystem(),
                            ENTITY_TYPE_ITEM,
                            sourceId,
                            targetItem.getId());
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
                && (targetItem.getVendor() == null
                        || !upsert.vendorId.equals(targetItem.getVendor().getId()))) {
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

        if (upsert.cogs != null
                && (targetItem.getCogs() == null
                        || upsert.cogs.compareTo(targetItem.getCogs()) != 0)) {
            targetItem.setCogs(upsert.cogs);
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

    private ItemUpsert parseItemUpsert(
            ItemRowContext ctx, Long sourceId, List<String> columns, List<String> values) {
        String name = normalizeBlankToNull(getByColumn(columns, values, "name"));
        if (name == null) {
            addSample(ctx.samples.warningSamples, "productId=" + sourceId + " missing name");
            return null;
        }

        String description = normalizeBlankToNull(getByColumn(columns, values, "description"));
        if (description != null) {
            ctx.unsupportedDescriptionNonEmpty.incrementAndGet();
            addUnsupportedSample(
                    ctx.unsupportedFieldSamples, "productId=" + sourceId + " has description");
        }
        String status = normalizeBlankToNull(getByColumn(columns, values, "status"));
        if (status != null) {
            ctx.unsupportedStatusPresent.incrementAndGet();
            addUnsupportedSample(
                    ctx.unsupportedFieldSamples, "productId=" + sourceId + " has status=" + status);
        }
        String cogsCandidate =
                firstNonBlank(
                        columns,
                        values,
                        List.of(
                                "purchase_price",
                                "purchase_cost",
                                "cost_price",
                                "cost",
                                "buying_price",
                                "cogs"));
        if (cogsCandidate != null) {
            ctx.unsupportedCogsCandidatesPresent.incrementAndGet();
            addUnsupportedSample(
                    ctx.unsupportedFieldSamples,
                    "productId=" + sourceId + " has cogsCandidate=" + cogsCandidate);
        }

        String sku = normalizeBlankToNull(getByColumn(columns, values, "sku"));
        String barcode = normalizeBlankToNull(getByColumn(columns, values, "barcode"));
        Long unitGroupId = asLong(getByColumn(columns, values, "unit_group"));
        Long taxGroupId = asLong(getByColumn(columns, values, "tax_group_id"));
        Long taxId = asLong(getByColumn(columns, values, "tax_id"));
        BigDecimal taxRate = resolveTaxRate(columns, values, ctx.lookups, taxGroupId, taxId);

        Long categoryId = asLong(getByColumn(columns, values, "category_id"));

        UUID vendorId = null;
        if (categoryId != null) {
            ctx.vendorLinkageProductsWithCategory.incrementAndGet();
            Optional<MigrationIdMap> vendorMap =
                    idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                            ctx.run.getSourceSystem(), ENTITY_TYPE_PARTY_VENDOR, categoryId);
            if (vendorMap.isPresent()) {
                vendorId = vendorMap.get().getTargetId();
                ctx.vendorLinkageMapped.incrementAndGet();
            } else {
                ctx.vendorLinkageMissingMap.incrementAndGet();
                addSample(
                        ctx.samples.warningSamples,
                        "productId="
                                + sourceId
                                + " missing vendor mapping for categoryId="
                                + categoryId);
            }
        } else {
            ctx.vendorLinkageMissingCategory.incrementAndGet();
        }

        String uom =
                unitGroupId == null
                        ? DEFAULT_UOM
                        : ctx.lookups.uomByGroupId.getOrDefault(unitGroupId, DEFAULT_UOM);
        String code = chooseItemCode(sku, barcode, sourceId);

        BigDecimal unitPrice = resolveSellingPrice(ctx, sourceId, columns, values);
        BigDecimal cogs = resolveCogs(ctx, sourceId, columns, values);

        ItemDto dto =
                ItemDto.builder()
                        .name(name)
                        .code(code)
                        .barcode(
                                barcode != null && itemRepository.existsByBarcode(barcode)
                                        ? null
                                        : barcode)
                        .uom(uom)
                        .unitPrice(unitPrice)
                        .taxRate(taxRate)
                        .cogs(cogs)
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

    private BigDecimal resolveSellingPrice(
            ItemRowContext ctx, Long productId, List<String> columns, List<String> values) {
        BigDecimal fromProduct =
                asBigDecimal(
                        firstNonBlank(
                                columns, values, List.of("sale_price", "selling_price", "price")),
                        null);
        if (fromProduct != null && fromProduct.compareTo(BigDecimal.ZERO) > 0) {
            ctx.priceFromProductColumns.incrementAndGet();
            return fromProduct;
        }
        BigDecimal fromOrders = ctx.lookups.avgOrderPriceByProductId.get(productId);
        if (fromOrders != null && fromOrders.compareTo(BigDecimal.ZERO) > 0) {
            ctx.priceFromOrders.incrementAndGet();
            return fromOrders;
        }
        ctx.priceDefaultedZero.incrementAndGet();
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveCogs(
            ItemRowContext ctx, Long productId, List<String> columns, List<String> values) {
        BigDecimal fromProduct =
                asBigDecimal(
                        firstNonBlank(
                                columns,
                                values,
                                List.of(
                                        "purchase_price",
                                        "purchase_cost",
                                        "cost_price",
                                        "cost",
                                        "buying_price")),
                        null);
        if (fromProduct != null && fromProduct.compareTo(BigDecimal.ZERO) > 0) {
            ctx.cogsFromProductColumns.incrementAndGet();
            return fromProduct;
        }
        BigDecimal fromProcurements = ctx.lookups.avgPurchasePriceByProductId.get(productId);
        if (fromProcurements != null && fromProcurements.compareTo(BigDecimal.ZERO) > 0) {
            ctx.cogsFromProcurements.incrementAndGet();
            return fromProcurements;
        }
        ctx.cogsDefaultedZero.incrementAndGet();
        return BigDecimal.ZERO;
    }

    private Map<Long, BigDecimal> resolveAverageOrderPriceByProductId(Path dumpPath)
            throws Exception {
        Map<Long, BigDecimal> sum = new HashMap<>();
        Map<Long, Long> count = new HashMap<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PRODUCTS,
                (columns, values) -> {
                    Long productId = asLong(getByColumn(columns, values, "product_id"));
                    BigDecimal price = asBigDecimal(getByColumn(columns, values, "price"), null);
                    if (productId == null || price == null) {
                        return;
                    }
                    sum.merge(productId, price, BigDecimal::add);
                    count.put(productId, count.getOrDefault(productId, 0L) + 1L);
                });
        Map<Long, BigDecimal> avg = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> e : sum.entrySet()) {
            Long pid = e.getKey();
            BigDecimal total = e.getValue();
            long n = count.getOrDefault(pid, 0L);
            if (n > 0) {
                avg.put(pid, total.divide(BigDecimal.valueOf(n), java.math.RoundingMode.HALF_UP));
            }
        }
        return avg;
    }

    private Map<Long, BigDecimal> resolveAveragePurchasePriceByProductId(Path dumpPath)
            throws Exception {
        Map<Long, BigDecimal> sum = new HashMap<>();
        Map<Long, Long> count = new HashMap<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                TABLE_PROCUREMENTS_PRODUCTS,
                (columns, values) -> {
                    Long productId = asLong(getByColumn(columns, values, "product_id"));
                    BigDecimal purchasePrice =
                            asBigDecimal(getByColumn(columns, values, "purchase_price"), null);
                    if (productId == null || purchasePrice == null) {
                        return;
                    }
                    sum.merge(productId, purchasePrice, BigDecimal::add);
                    count.put(productId, count.getOrDefault(productId, 0L) + 1L);
                });
        Map<Long, BigDecimal> avg = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> e : sum.entrySet()) {
            Long pid = e.getKey();
            BigDecimal total = e.getValue();
            long n = count.getOrDefault(pid, 0L);
            if (n > 0) {
                avg.put(pid, total.divide(BigDecimal.valueOf(n), java.math.RoundingMode.HALF_UP));
            }
        }
        return avg;
    }

    private BigDecimal resolveTaxRate(
            List<String> columns,
            List<String> values,
            ItemImportLookups lookups,
            Long taxGroupId,
            Long taxId) {
        if (taxGroupId != null && lookups.taxRateByTaxGroupId.containsKey(taxGroupId)) {
            return lookups.taxRateByTaxGroupId.get(taxGroupId);
        }
        if (taxId != null && lookups.taxRateByTaxId.containsKey(taxId)) {
            return lookups.taxRateByTaxId.get(taxId);
        }
        return asBigDecimal(getByColumn(columns, values, "tax_value"), DEFAULT_TAX_RATE);
    }

    private Map<Long, BigDecimal> resolveTaxRateByTaxId(Path dumpPath) throws Exception {
        Map<Long, BigDecimal> rateByTaxId = new HashMap<>();
        dumpSql.forEachInsertRow(
                dumpPath,
                TABLE_TAXES,
                (columns, values) -> {
                    Long taxId = asLong(getByColumn(columns, values, "id"));
                    BigDecimal rate = asBigDecimal(getByColumn(columns, values, "rate"), null);
                    if (taxId == null || rate == null) {
                        return;
                    }
                    rateByTaxId.put(taxId, rate);
                });
        return rateByTaxId;
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
                    String identifier =
                            normalizeBlankToNull(getByColumn(columns, values, "identifier"));
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
        if (candidate.length() < existing.length()) {
            return candidate;
        }
        return existing;
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

    private String firstNonBlank(
            List<String> columns, List<String> values, List<String> candidateColumns) {
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

    private void addUnsupportedSample(List<String> samples, String value) {
        if (samples.size() < MAX_UNSUPPORTED_SAMPLES) {
            samples.add(value);
        }
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

    private void ensureDumpExists(Path dumpPath) {
        if (!Files.exists(dumpPath)) {
            throw new IllegalArgumentException("Dump file not found: " + dumpPath);
        }
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

    private static final class ItemImportLookups {
        private final Map<Long, String> uomByGroupId;
        private final Map<Long, BigDecimal> taxRateByTaxId;
        private final Map<Long, BigDecimal> taxRateByTaxGroupId;
        private final Map<Long, BigDecimal> avgOrderPriceByProductId;
        private final Map<Long, BigDecimal> avgPurchasePriceByProductId;

        private ItemImportLookups(
                Map<Long, String> uomByGroupId,
                Map<Long, BigDecimal> taxRateByTaxId,
                Map<Long, BigDecimal> taxRateByTaxGroupId,
                Map<Long, BigDecimal> avgOrderPriceByProductId,
                Map<Long, BigDecimal> avgPurchasePriceByProductId) {
            this.uomByGroupId = uomByGroupId;
            this.taxRateByTaxId = taxRateByTaxId;
            this.taxRateByTaxGroupId = taxRateByTaxGroupId;
            this.avgOrderPriceByProductId = avgOrderPriceByProductId;
            this.avgPurchasePriceByProductId = avgPurchasePriceByProductId;
        }
    }

    private static final class ItemUpsert {
        private ItemDto dto;
        private String code;
        private UUID vendorId;
        private String uom;
        private BigDecimal taxRate;
        private BigDecimal unitPrice;
        private BigDecimal cogs;
        private String barcode;
    }

    private static final class ItemRowContext {
        private final MigrationRun run;
        private final ItemImportLookups lookups;
        private final ImportItemsCounters counters;
        private final ItemImportSamples samples;
        private final List<String> unsupportedFieldSamples;

        private final AtomicLong vendorLinkageProductsWithCategory = new AtomicLong();
        private final AtomicLong vendorLinkageMapped = new AtomicLong();
        private final AtomicLong vendorLinkageMissingMap = new AtomicLong();
        private final AtomicLong vendorLinkageMissingCategory = new AtomicLong();

        private final AtomicLong unsupportedDescriptionNonEmpty = new AtomicLong();
        private final AtomicLong unsupportedStatusPresent = new AtomicLong();
        private final AtomicLong unsupportedCogsCandidatesPresent = new AtomicLong();
        private final AtomicLong priceFromProductColumns = new AtomicLong();
        private final AtomicLong priceFromOrders = new AtomicLong();
        private final AtomicLong priceDefaultedZero = new AtomicLong();
        private final AtomicLong cogsFromProductColumns = new AtomicLong();
        private final AtomicLong cogsFromProcurements = new AtomicLong();
        private final AtomicLong cogsDefaultedZero = new AtomicLong();

        private ItemRowContext(
                MigrationRun run,
                ItemImportLookups lookups,
                ImportItemsCounters counters,
                ItemImportSamples samples,
                List<String> unsupportedFieldSamples) {
            this.run = run;
            this.lookups = lookups;
            this.counters = counters;
            this.samples = samples;
            this.unsupportedFieldSamples = unsupportedFieldSamples;
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

    private static final class OpeningStockCounters {
        private final AtomicLong found = new AtomicLong();
        private final AtomicLong inScope = new AtomicLong();
        private final AtomicLong skippedOutOfScope = new AtomicLong();
        private final AtomicLong skippedOverLimit = new AtomicLong();
        private final AtomicLong alreadyMapped = new AtomicLong();
        private final AtomicLong skippedMissingItemMap = new AtomicLong();
        private final AtomicLong skippedMissingQuantity = new AtomicLong();
        private final AtomicLong skippedNonPositiveQuantity = new AtomicLong();
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
