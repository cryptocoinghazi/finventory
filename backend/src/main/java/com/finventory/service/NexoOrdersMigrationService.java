package com.finventory.service;

import com.finventory.dto.PartyDto;
import com.finventory.dto.SalesInvoiceDto;
import com.finventory.dto.SalesInvoiceLineDto;
import com.finventory.dto.WarehouseDto;
import com.finventory.model.InvoicePaymentStatus;
import com.finventory.model.MigrationIdMap;
import com.finventory.model.MigrationLogEntry;
import com.finventory.model.MigrationLogLevel;
import com.finventory.model.MigrationRun;
import com.finventory.model.MigrationStageKey;
import com.finventory.model.Party;
import com.finventory.repository.MigrationIdMapRepository;
import com.finventory.repository.MigrationLogEntryRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NexoOrdersMigrationService {
    private static final String ENTITY_TYPE_ITEM = "ITEM";
    private static final String ENTITY_TYPE_PARTY_CUSTOMER = "PARTY_CUSTOMER";
    private static final String ENTITY_TYPE_SALES_INVOICE = "SALES_INVOICE";

    private static final String TABLE_ORDERS = "ns_nexopos_orders";
    private static final String TABLE_ORDERS_PRODUCTS = "ns_nexopos_orders_products";
    private static final String TABLE_ORDERS_PAYMENTS = "ns_nexopos_orders_payments";

    private static final BigDecimal DEFAULT_UNIT_PRICE = BigDecimal.ZERO;
    private static final int MAX_ERROR_SAMPLES = 25;

    private static final int DATE_PREFIX_LENGTH = 10;
    private static final int DATE_YEAR_SEPARATOR_INDEX = 4;
    private static final int DATE_MONTH_SEPARATOR_INDEX = 7;
    private static final int LEGACY_INVOICE_NUMBER_MAX_LENGTH = 50;
    private static final int MONEY_SCALE = 2;

    private final WarehouseRepository warehouseRepository;
    private final PartyRepository partyRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final MigrationIdMapRepository idMapRepository;
    private final MigrationLogEntryRepository logEntryRepository;
    private final WarehouseService warehouseService;
    private final PartyService partyService;
    private final SalesInvoiceService salesInvoiceService;
    private final NexoDumpSqlService dumpSqlService;

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

    @Transactional
    public Map<String, Object> importSalesPilot(MigrationRun run, Path dumpPath) throws Exception {
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

        SalesPilotCounters counters = new SalesPilotCounters();
        List<String> warningSamples = new ArrayList<>();
        List<String> errorSamples = new ArrayList<>();
        Map<Long, OrderHeader> eligibleOrdersById = new LinkedHashMap<>();
        SalesPilotContext ctx =
                new SalesPilotContext(run, warehouseId, counters, warningSamples, errorSamples, eligibleOrdersById);

        collectEligiblePaidOrders(ctx, dumpPath);

        if (eligibleOrdersById.isEmpty()) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("implemented", true);
            stats.put("stage", MigrationStageKey.IMPORT_SALES_PILOT.name());
            stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
            stats.put("dryRun", run.isDryRun());
            stats.put("message", "No eligible paid orders found");
            stats.put("insertStatements", dumpSqlService.countInsertStatements(dumpPath, TABLE_ORDERS));
            return stats;
        }

        Map<Long, List<OrderLine>> linesByOrderId = collectOrderLines(dumpPath, eligibleOrdersById);
        Map<Long, BigDecimal> paymentsByOrderId = collectPayments(dumpPath, eligibleOrdersById);

        importEligibleOrders(ctx, linesByOrderId);

        Map<String, Object> stats = buildSalesPilotStats(ctx, dumpPath, linesByOrderId, paymentsByOrderId);

        log(
                run,
                MigrationStageKey.IMPORT_SALES_PILOT,
                MigrationLogLevel.INFO,
                "Sales pilot import finished",
                "found="
                        + counters.found.get()
                        + ", created="
                        + counters.created.get()
                        + ", markedPaid="
                        + counters.markedPaid.get()
                        + ", dryRun="
                        + run.isDryRun()
                        + ", limit="
                        + run.getScopeLimit());

        return stats;
    }

    private void collectEligiblePaidOrders(SalesPilotContext ctx, Path dumpPath) throws Exception {
        dumpSqlService.forEachInsertRow(
                dumpPath,
                TABLE_ORDERS,
                (columns, values) -> {
                    Long orderId = asLong(dumpSqlService.getByColumn(columns, values, "id"));
                    if (orderId == null) {
                        return;
                    }
                    ctx.counters.found.incrementAndGet();

                    if (!isInScope(ctx.run, orderId)) {
                        ctx.counters.skippedOutOfScope.incrementAndGet();
                        return;
                    }

                    Integer scopeLimit = ctx.run.getScopeLimit();
                    if (scopeLimit != null && ctx.counters.inScope.get() >= scopeLimit) {
                        ctx.counters.skippedOverLimit.incrementAndGet();
                        return;
                    }

                    ctx.counters.inScope.incrementAndGet();

                    String paymentStatus =
                            normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, "payment_status"));
                    if (!isPaidStatus(paymentStatus)) {
                        ctx.counters.skippedNotPaid.incrementAndGet();
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
                    header.customerId = asLong(dumpSqlService.getByColumn(columns, values, "customer_id"));
                    header.createdAt =
                            normalizeBlankToNull(
                                    firstNonBlank(columns, values, List.of("created_at", "date", "created")));
                    ctx.eligibleOrdersById.put(orderId, header);
                });
    }

    private Map<Long, List<OrderLine>> collectOrderLines(Path dumpPath, Map<Long, OrderHeader> eligibleOrdersById)
            throws Exception {
        Map<Long, List<OrderLine>> linesByOrderId = new HashMap<>();
        dumpSqlService.forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PRODUCTS,
                (columns, values) -> {
                    Long orderId = asLong(dumpSqlService.getByColumn(columns, values, "order_id"));
                    if (orderId == null || !eligibleOrdersById.containsKey(orderId)) {
                        return;
                    }

                    Long productId = asLong(dumpSqlService.getByColumn(columns, values, "product_id"));
                    BigDecimal quantity =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "quantity"), BigDecimal.ZERO);
                    BigDecimal unitPrice = asBigDecimal(dumpSqlService.getByColumn(columns, values, "price"), null);
                    BigDecimal totalPrice =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "total_price"), null);

                    OrderLine line = new OrderLine();
                    line.productId = productId;
                    line.quantity = quantity;
                    line.unitPrice = unitPrice;
                    line.totalPrice = totalPrice;

                    linesByOrderId.computeIfAbsent(orderId, ignored -> new ArrayList<>()).add(line);
                });
        return linesByOrderId;
    }

    private Map<Long, BigDecimal> collectPayments(Path dumpPath, Map<Long, OrderHeader> eligibleOrdersById)
            throws Exception {
        Map<Long, BigDecimal> paymentsByOrderId = new HashMap<>();
        dumpSqlService.forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PAYMENTS,
                (columns, values) -> {
                    Long orderId = asLong(dumpSqlService.getByColumn(columns, values, "order_id"));
                    if (orderId == null || !eligibleOrdersById.containsKey(orderId)) {
                        return;
                    }

                    BigDecimal value =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "value"), BigDecimal.ZERO);
                    paymentsByOrderId.merge(orderId, value, BigDecimal::add);
                });
        return paymentsByOrderId;
    }

    private void importEligibleOrders(SalesPilotContext ctx, Map<Long, List<OrderLine>> linesByOrderId) {
        for (OrderHeader header : ctx.eligibleOrdersById.values()) {
            importSingleOrder(ctx, header, linesByOrderId.getOrDefault(header.orderId, List.of()));
        }
    }

    private void importSingleOrder(SalesPilotContext ctx, OrderHeader header, List<OrderLine> orderLines) {
        Optional<MigrationIdMap> existingInvoiceMap =
                idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                        ctx.run.getSourceSystem(), ENTITY_TYPE_SALES_INVOICE, header.orderId);
        if (existingInvoiceMap.isPresent()) {
            ctx.counters.alreadyMapped.incrementAndGet();
            return;
        }

        if (orderLines.isEmpty()) {
            ctx.counters.skippedNoLines.incrementAndGet();
            addSample(ctx.warningSamples, "orderId=" + header.orderId + " has no lines");
            return;
        }

        List<SalesInvoiceLineDto> invoiceLines = buildInvoiceLines(ctx, header, orderLines);
        if (invoiceLines == null) {
            ctx.counters.skippedMissingItems.incrementAndGet();
            addSample(ctx.warningSamples, "orderId=" + header.orderId + " missing item mappings");
            return;
        }
        if (invoiceLines.isEmpty()) {
            ctx.counters.skippedNoLines.incrementAndGet();
            return;
        }

        UUID partyId = resolveCustomerPartyId(ctx.run, header.customerId, ctx.warningSamples);
        if (partyId == null) {
            ctx.counters.skippedMissingParty.incrementAndGet();
            addSample(ctx.warningSamples, "orderId=" + header.orderId + " missing customer/party mapping");
            return;
        }

        LocalDate invoiceDate = parseLocalDateOrToday(header.createdAt);
        String invoiceNumber = buildLegacyInvoiceNumber(header);

        var existingByInvoiceNumber = salesInvoiceRepository.findByInvoiceNumber(invoiceNumber);
        if (existingByInvoiceNumber.isPresent()) {
            ctx.counters.linkedExisting.incrementAndGet();
            if (!ctx.run.isDryRun()) {
                saveIdMap(
                        ctx.run.getSourceSystem(),
                        ENTITY_TYPE_SALES_INVOICE,
                        header.orderId,
                        existingByInvoiceNumber.get().getId());
            }
            return;
        }

        if (ctx.run.isDryRun()) {
            ctx.counters.wouldCreate.incrementAndGet();
            return;
        }

        try {
            SalesInvoiceDto createdInvoice =
                    salesInvoiceService.createSalesInvoice(
                            SalesInvoiceDto.builder()
                                    .invoiceNumber(invoiceNumber)
                                    .invoiceDate(invoiceDate)
                                    .partyId(partyId)
                                    .warehouseId(ctx.warehouseId)
                                    .paymentStatus(InvoicePaymentStatus.PENDING)
                                    .lines(invoiceLines)
                                    .build());

            ctx.counters.created.incrementAndGet();
            saveIdMap(
                    ctx.run.getSourceSystem(),
                    ENTITY_TYPE_SALES_INVOICE,
                    header.orderId,
                    createdInvoice.getId());

            salesInvoiceService.applyPayment(
                    createdInvoice.getId(), InvoicePaymentStatus.PAID, BigDecimal.ZERO);
            ctx.counters.markedPaid.incrementAndGet();
        } catch (Exception e) {
            ctx.counters.errors.incrementAndGet();
            addSample(ctx.errorSamples, "orderId=" + header.orderId + ": " + e.getMessage());
        }
    }

    private List<SalesInvoiceLineDto> buildInvoiceLines(
            SalesPilotContext ctx, OrderHeader header, List<OrderLine> orderLines) {
        List<SalesInvoiceLineDto> invoiceLines = new ArrayList<>();
        boolean missingAnyItem = false;

        for (OrderLine l : orderLines) {
            if (l.productId == null) {
                missingAnyItem = true;
                continue;
            }
            Optional<MigrationIdMap> itemMap =
                    idMapRepository.findBySourceSystemAndEntityTypeAndSourceId(
                            ctx.run.getSourceSystem(), ENTITY_TYPE_ITEM, l.productId);
            if (itemMap.isEmpty()) {
                missingAnyItem = true;
                continue;
            }
            BigDecimal qty = l.quantity != null ? l.quantity : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal unitPrice = resolveUnitPrice(l, qty);
            invoiceLines.add(
                    SalesInvoiceLineDto.builder()
                            .itemId(itemMap.get().getTargetId())
                            .quantity(qty)
                            .unitPrice(unitPrice)
                            .build());
        }

        if (missingAnyItem) {
            return null;
        }
        return invoiceLines;
    }

    private BigDecimal resolveUnitPrice(OrderLine line, BigDecimal qty) {
        BigDecimal unitPrice = line.unitPrice;
        if (unitPrice == null && line.totalPrice != null) {
            unitPrice = line.totalPrice.divide(qty, MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (unitPrice == null) {
            unitPrice = DEFAULT_UNIT_PRICE;
        }
        return unitPrice;
    }

    private Map<String, Object> buildSalesPilotStats(
            SalesPilotContext ctx,
            Path dumpPath,
            Map<Long, List<OrderLine>> linesByOrderId,
            Map<Long, BigDecimal> paymentsByOrderId)
            throws Exception {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("implemented", true);
        stats.put("stage", MigrationStageKey.IMPORT_SALES_PILOT.name());
        stats.put("dumpPath", dumpPath.toAbsolutePath().normalize().toString());
        stats.put("dryRun", ctx.run.isDryRun());
        stats.put("sourceSystem", ctx.run.getSourceSystem());
        stats.put("sourceReference", ctx.run.getSourceReference());
        stats.put("scopeSourceIdMin", ctx.run.getScopeSourceIdMin());
        stats.put("scopeSourceIdMax", ctx.run.getScopeSourceIdMax());
        stats.put("scopeLimit", ctx.run.getScopeLimit());
        stats.put("warehouseId", ctx.warehouseId);
        stats.put("eligiblePaidOrders", ctx.eligibleOrdersById.size());
        stats.put("eligibleOrdersWithLines", linesByOrderId.size());
        stats.put("eligibleOrdersWithPayments", paymentsByOrderId.size());
        stats.put("insertStatementsOrders", dumpSqlService.countInsertStatements(dumpPath, TABLE_ORDERS));
        stats.put(
                "insertStatementsOrderLines",
                dumpSqlService.countInsertStatements(dumpPath, TABLE_ORDERS_PRODUCTS));
        stats.put("insertStatementsPayments", dumpSqlService.countInsertStatements(dumpPath, TABLE_ORDERS_PAYMENTS));
        stats.put("found", ctx.counters.found.get());
        stats.put("inScope", ctx.counters.inScope.get());
        stats.put("skippedOutOfScope", ctx.counters.skippedOutOfScope.get());
        stats.put("skippedOverLimit", ctx.counters.skippedOverLimit.get());
        stats.put("skippedNotPaid", ctx.counters.skippedNotPaid.get());
        stats.put("skippedMissingItems", ctx.counters.skippedMissingItems.get());
        stats.put("skippedMissingParty", ctx.counters.skippedMissingParty.get());
        stats.put("skippedNoLines", ctx.counters.skippedNoLines.get());
        stats.put("alreadyMapped", ctx.counters.alreadyMapped.get());
        stats.put("linkedExisting", ctx.counters.linkedExisting.get());
        stats.put("created", ctx.counters.created.get());
        stats.put("wouldCreate", ctx.counters.wouldCreate.get());
        stats.put("markedPaid", ctx.counters.markedPaid.get());
        stats.put("warnings", ctx.warningSamples.size());
        stats.put("warningSamples", ctx.warningSamples);
        stats.put("errors", ctx.counters.errors.get());
        stats.put("errorSamples", ctx.errorSamples);
        return stats;
    }

    private void addSample(List<String> samples, String message) {
        if (samples.size() < MAX_ERROR_SAMPLES) {
            samples.add(message);
        }
    }

    private static final class SalesPilotCounters {
        private final AtomicLong found = new AtomicLong();
        private final AtomicLong inScope = new AtomicLong();
        private final AtomicLong skippedOutOfScope = new AtomicLong();
        private final AtomicLong skippedOverLimit = new AtomicLong();
        private final AtomicLong skippedNotPaid = new AtomicLong();
        private final AtomicLong skippedMissingItems = new AtomicLong();
        private final AtomicLong skippedMissingParty = new AtomicLong();
        private final AtomicLong skippedNoLines = new AtomicLong();
        private final AtomicLong alreadyMapped = new AtomicLong();
        private final AtomicLong linkedExisting = new AtomicLong();
        private final AtomicLong created = new AtomicLong();
        private final AtomicLong wouldCreate = new AtomicLong();
        private final AtomicLong markedPaid = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
    }

    private static final class SalesPilotContext {
        private final MigrationRun run;
        private final UUID warehouseId;
        private final SalesPilotCounters counters;
        private final List<String> warningSamples;
        private final List<String> errorSamples;
        private final Map<Long, OrderHeader> eligibleOrdersById;

        private SalesPilotContext(
                MigrationRun run,
                UUID warehouseId,
                SalesPilotCounters counters,
                List<String> warningSamples,
                List<String> errorSamples,
                Map<Long, OrderHeader> eligibleOrdersById) {
            this.run = run;
            this.warehouseId = warehouseId;
            this.counters = counters;
            this.warningSamples = warningSamples;
            this.errorSamples = errorSamples;
            this.eligibleOrdersById = eligibleOrdersById;
        }
    }

    private String firstNonBlank(List<String> columns, List<String> values, List<String> candidateColumns) {
        for (String col : candidateColumns) {
            if (!columns.contains(col)) {
                continue;
            }
            String v = normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, col));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private UUID resolveDefaultWarehouseId(MigrationRun run) {
        List<com.finventory.model.Warehouse> warehouses =
                warehouseRepository.findAll(PageRequest.of(0, 1)).getContent();
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
        if (v.length() >= DATE_PREFIX_LENGTH
                && v.charAt(DATE_YEAR_SEPARATOR_INDEX) == '-'
                && v.charAt(DATE_MONTH_SEPARATOR_INDEX) == '-') {
            try {
                return LocalDate.parse(v.substring(0, DATE_PREFIX_LENGTH));
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
            if (sb.length() >= LEGACY_INVOICE_NUMBER_MAX_LENGTH) {
                break;
            }
        }
        if (sb.length() <= "NEXO-".length()) {
            return "NEXO-ORDER-" + header.orderId;
        }
        return sb.toString();
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
            MigrationRun run, MigrationStageKey stageKey, MigrationLogLevel level, String message, String details) {
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
}
