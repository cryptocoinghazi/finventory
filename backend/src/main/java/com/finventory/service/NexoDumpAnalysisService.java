package com.finventory.service;

import com.finventory.model.MigrationRun;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NexoDumpAnalysisService {
    private static final String TABLE_PRODUCTS = "ns_nexopos_products";
    private static final String TABLE_UNITS = "ns_nexopos_units";
    private static final String TABLE_ORDERS = "ns_nexopos_orders";
    private static final String TABLE_ORDERS_PRODUCTS = "ns_nexopos_orders_products";
    private static final String TABLE_ORDERS_PAYMENTS = "ns_nexopos_orders_payments";
    private static final String TABLE_CUSTOMERS = "ns_nexopos_customers";
    private static final String TABLE_PROVIDERS = "ns_nexopos_providers";
    private static final String TABLE_TAXES = "ns_nexopos_taxes";
    private static final int ANALYZE_SAMPLE_ROWS = 3;
    private static final int MAX_CAPTURED_ORDER_IDS = 50_000;

    private final NexoDumpSqlService dumpSqlService;

    public Map<String, Object> analyzeSource(Path dumpPath) throws Exception {
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

        stats.put(
                TABLE_ORDERS,
                analyzeTable(
                        dumpPath,
                        TABLE_ORDERS,
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
                TABLE_ORDERS_PRODUCTS,
                analyzeTable(
                        dumpPath,
                        TABLE_ORDERS_PRODUCTS,
                        List.of("order_id", "product_id", "quantity", "price", "total_price"),
                        List.of()));

        stats.put(
                TABLE_ORDERS_PAYMENTS,
                analyzeTable(
                        dumpPath,
                        TABLE_ORDERS_PAYMENTS,
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

    public Map<String, Object> analyzeOrders(MigrationRun run, Path dumpPath) throws Exception {
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

        dumpSqlService.forEachInsertRow(
                dumpPath,
                TABLE_ORDERS,
                (columns, values) -> {
                    Long orderId = asLong(dumpSqlService.getByColumn(columns, values, "id"));
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

                    BigDecimal subtotal =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "subtotal"), BigDecimal.ZERO);
                    BigDecimal total =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "total"), BigDecimal.ZERO);
                    BigDecimal tax =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "tax_value"), BigDecimal.ZERO);
                    sumSubtotal[0] = sumSubtotal[0].add(subtotal);
                    sumTotal[0] = sumTotal[0].add(total);
                    sumTax[0] = sumTax[0].add(tax);

                    String createdAt = normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, "created_at"));
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
                        String v = normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, col));
                        String key = v == null ? "<NULL>" : v;
                        e.getValue().computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
                    }

                    if (sampleRows.size() < ANALYZE_SAMPLE_ROWS) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String col : List.of("id", "code", "customer_id", "total", "tax_value", "created_at")) {
                            if (columns.contains(col)) {
                                row.put(col, normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, col)));
                            }
                        }
                        sampleRows.add(row);
                    }
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("insertStatements", dumpSqlService.countInsertStatements(dumpPath, TABLE_ORDERS));
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

        dumpSqlService.forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PRODUCTS,
                (columns, values) -> {
                    Long orderId = asLong(dumpSqlService.getByColumn(columns, values, "order_id"));
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
                    BigDecimal qty =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "quantity"), BigDecimal.ZERO);
                    BigDecimal total =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "total_price"), BigDecimal.ZERO);
                    sumQuantity[0] = sumQuantity[0].add(qty);
                    sumTotalPrice[0] = sumTotalPrice[0].add(total);
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("insertStatements", dumpSqlService.countInsertStatements(dumpPath, TABLE_ORDERS_PRODUCTS));
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

        dumpSqlService.forEachInsertRow(
                dumpPath,
                TABLE_ORDERS_PAYMENTS,
                (columns, values) -> {
                    Long orderId = asLong(dumpSqlService.getByColumn(columns, values, "order_id"));
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
                    BigDecimal value =
                            asBigDecimal(dumpSqlService.getByColumn(columns, values, "value"), BigDecimal.ZERO);
                    sumValue[0] = sumValue[0].add(value);

                    String identifier = normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, "identifier"));
                    String key = identifier == null ? "<NULL>" : identifier;
                    identifiers.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
                });

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("insertStatements", dumpSqlService.countInsertStatements(dumpPath, TABLE_ORDERS_PAYMENTS));
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

    private Map<String, Object> analyzeTable(
            Path dumpPath, String tableName, List<String> importantColumns, List<String> statusColumns)
            throws Exception {
        long insertStatements = dumpSqlService.countInsertStatements(dumpPath, tableName);

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

        dumpSqlService.forEachInsertRow(
                dumpPath,
                tableName,
                (columns, values) -> {
                    if (insertColumns.isEmpty() && !columns.isEmpty()) {
                        insertColumns.addAll(columns);
                    }
                    tupleCount[0]++;

                    Long id = asLong(dumpSqlService.getByColumn(columns, values, "id"));
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
                        String v = normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, col));
                        if (v == null) {
                            nullCounts.get(col).incrementAndGet();
                        }
                    }

                    for (String col : statusColumns) {
                        if (!columns.contains(col)) {
                            continue;
                        }
                        String v = normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, col));
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
                                row.put(col, normalizeBlankToNull(dumpSqlService.getByColumn(columns, values, col)));
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

    private String normalizeStatusKey(String value) {
        String v = normalizeBlankToNull(value);
        if (v == null) {
            return "<NULL>";
        }
        return v.toUpperCase(Locale.ROOT);
    }
}

