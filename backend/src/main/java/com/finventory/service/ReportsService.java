package com.finventory.service;

import com.finventory.dto.ActivityFeedEntryDto;
import com.finventory.dto.AnnualReportDto;
import com.finventory.dto.PartyLedgerEntryDto;
import com.finventory.dto.PartyOutstandingDto;
import com.finventory.dto.ProfitLossReportDto;
import com.finventory.dto.SalesReportDto;
import com.finventory.dto.StockReportDto;
import com.finventory.dto.StockSummaryDto;
import com.finventory.dto.SystemStatusDto;
import com.finventory.model.GLTransaction;
import com.finventory.model.InvoicePaymentStatus;
import com.finventory.model.Party;
import com.finventory.model.SalesInvoice;
import com.finventory.model.SalesInvoiceLine;
import com.finventory.repository.GLTransactionRepository;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.PurchaseInvoiceRepository;
import com.finventory.repository.PurchaseReturnRepository;
import com.finventory.repository.SalesInvoiceRepository;
import com.finventory.repository.SalesReturnRepository;
import com.finventory.repository.StockAdjustmentRepository;
import com.finventory.repository.StockLedgerRepository;
import com.finventory.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private static final int MAX_ACTIVITY_LIMIT = 50;
    private static final long AGE_BUCKET_0_30_MAX = 30L;
    private static final long AGE_BUCKET_31_60_MAX = 60L;
    private static final long AGE_BUCKET_61_90_MAX = 90L;
    private static final int MONTHS_IN_YEAR = 12;
    private static final int DAYS_IN_WEEK_MINUS_ONE = 6;

    private final StockLedgerRepository stockLedgerRepository;
    private final GLTransactionRepository glTransactionRepository;
    private final com.finventory.repository.GLLineRepository glLineRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;
    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    public com.finventory.dto.DashboardStatsDto getDashboardStats() {
        java.math.BigDecimal salesToday = salesInvoiceRepository.findTotalSalesToday();
        java.math.BigDecimal purchaseToday = purchaseInvoiceRepository.findTotalPurchaseToday();
        java.math.BigDecimal stockValue = stockLedgerRepository.findTotalStockValue();
        java.math.BigDecimal outstanding = glLineRepository.findTotalReceivables();

        return com.finventory.dto.DashboardStatsDto.builder()
                .salesToday(salesToday != null ? salesToday : java.math.BigDecimal.ZERO)
                .purchaseToday(purchaseToday != null ? purchaseToday : java.math.BigDecimal.ZERO)
                .stockValue(stockValue != null ? stockValue : java.math.BigDecimal.ZERO)
                .outstanding(outstanding != null ? outstanding : java.math.BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StockSummaryDto> getStockSummary() {
        return stockLedgerRepository.findStockSummary();
    }

    @Transactional(readOnly = true)
    public List<PartyOutstandingDto> getPartyOutstanding() {
        return getPartyOutstanding(null, null, null, null, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<PartyOutstandingDto> getPartyOutstanding(
            LocalDate fromDate,
            LocalDate toDate,
            Party.PartyType partyType,
            java.math.BigDecimal minOutstanding,
            LocalDate asOfDate) {
        LocalDate effectiveAsOf = asOfDate != null ? asOfDate : LocalDate.now();
        LocalDate effectiveTo = toDate != null ? toDate : effectiveAsOf;

        List<GLTransaction> transactions =
                glTransactionRepository.findOutstandingTransactions(
                        fromDate, effectiveTo, partyType);

        Map<UUID, PartyOutstandingDto> byParty = new LinkedHashMap<>();

        for (GLTransaction t : transactions) {
            Party party = t.getParty();
            if (party == null) {
                continue;
            }

            java.math.BigDecimal netAmount = getOutstandingNetAmount(t);
            if (netAmount.compareTo(java.math.BigDecimal.ZERO) == 0) {
                continue;
            }

            PartyOutstandingDto agg =
                    byParty.computeIfAbsent(
                            party.getId(),
                            (id) ->
                                    PartyOutstandingDto.builder()
                                            .partyId(party.getId())
                                            .partyName(party.getName())
                                            .partyType(party.getType().name())
                                            .phone(party.getPhone())
                                            .gstin(party.getGstin())
                                            .totalReceivable(java.math.BigDecimal.ZERO)
                                            .totalPayable(java.math.BigDecimal.ZERO)
                                            .netBalance(java.math.BigDecimal.ZERO)
                                            .age0to30(java.math.BigDecimal.ZERO)
                                            .age31to60(java.math.BigDecimal.ZERO)
                                            .age61to90(java.math.BigDecimal.ZERO)
                                            .age90Plus(java.math.BigDecimal.ZERO)
                                            .build());

            agg.setNetBalance(agg.getNetBalance().add(netAmount));

            long ageDays = ChronoUnit.DAYS.between(t.getDate(), effectiveAsOf);
            if (ageDays < 0) {
                ageDays = 0;
            }

            if (ageDays <= AGE_BUCKET_0_30_MAX) {
                agg.setAge0to30(agg.getAge0to30().add(netAmount));
            } else if (ageDays <= AGE_BUCKET_31_60_MAX) {
                agg.setAge31to60(agg.getAge31to60().add(netAmount));
            } else if (ageDays <= AGE_BUCKET_61_90_MAX) {
                agg.setAge61to90(agg.getAge61to90().add(netAmount));
            } else {
                agg.setAge90Plus(agg.getAge90Plus().add(netAmount));
            }
        }

        for (PartyOutstandingDto row : byParty.values()) {
            java.math.BigDecimal net =
                    row.getNetBalance() != null ? row.getNetBalance() : java.math.BigDecimal.ZERO;
            if (net.compareTo(java.math.BigDecimal.ZERO) > 0) {
                row.setTotalReceivable(net);
                row.setTotalPayable(java.math.BigDecimal.ZERO);
            } else if (net.compareTo(java.math.BigDecimal.ZERO) < 0) {
                row.setTotalReceivable(java.math.BigDecimal.ZERO);
                row.setTotalPayable(net.abs());
            } else {
                row.setTotalReceivable(java.math.BigDecimal.ZERO);
                row.setTotalPayable(java.math.BigDecimal.ZERO);
            }
        }

        List<PartyOutstandingDto> out = new ArrayList<>(byParty.values());
        out.removeIf(
                (r) ->
                        r.getNetBalance() == null
                                || r.getNetBalance().compareTo(java.math.BigDecimal.ZERO) == 0);
        if (minOutstanding != null && minOutstanding.compareTo(java.math.BigDecimal.ZERO) > 0) {
            out.removeIf((r) -> r.getNetBalance().abs().compareTo(minOutstanding) < 0);
        }

        out.sort(
                Comparator.comparing(
                        PartyOutstandingDto::getPartyName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    @Transactional(readOnly = true)
    public List<PartyLedgerEntryDto> getPartyOutstandingLedger(
            UUID partyId, LocalDate fromDate, LocalDate toDate) {
        if (partyId == null) {
            throw new IllegalArgumentException("partyId is required");
        }

        List<GLTransaction> transactions =
                glTransactionRepository.findOutstandingTransactionsForParty(
                        partyId, fromDate, toDate);

        List<PartyLedgerEntryDto> entries = new ArrayList<>();
        for (GLTransaction t : transactions) {
            java.math.BigDecimal netAmount = getOutstandingNetAmount(t);
            if (netAmount.compareTo(java.math.BigDecimal.ZERO) == 0) {
                continue;
            }

            entries.add(
                    PartyLedgerEntryDto.builder()
                            .date(t.getDate())
                            .refType(t.getRefType() != null ? t.getRefType().name() : null)
                            .refId(t.getRefId())
                            .description(t.getDescription())
                            .amount(netAmount)
                            .build());
        }
        entries.sort(
                Comparator.comparing(
                                PartyLedgerEntryDto::getDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed());
        return entries;
    }

    private static java.math.BigDecimal getOutstandingNetAmount(GLTransaction transaction) {
        java.math.BigDecimal net = java.math.BigDecimal.ZERO;
        if (transaction.getLines() == null) {
            return net;
        }

        for (var line : transaction.getLines()) {
            if (line == null) {
                continue;
            }
            String head = line.getAccountHead();
            if (!"ACCOUNTS_RECEIVABLE".equals(head) && !"ACCOUNTS_PAYABLE".equals(head)) {
                continue;
            }
            java.math.BigDecimal debit =
                    line.getDebit() != null ? line.getDebit() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal credit =
                    line.getCredit() != null ? line.getCredit() : java.math.BigDecimal.ZERO;
            net = net.add(debit.subtract(credit));
        }
        return net;
    }

    @Transactional(readOnly = true)
    public List<com.finventory.dto.GstRegisterEntryDto> getGstr1() {
        return salesInvoiceRepository.findGstr1Data();
    }

    @Transactional(readOnly = true)
    public List<com.finventory.dto.GstRegisterEntryDto> getGstr2() {
        return purchaseInvoiceRepository.findGstr2Data();
    }

    @Transactional(readOnly = true)
    public com.finventory.dto.Gstr3bDto getGstr3b() {
        List<Object[]> salesDataList = salesInvoiceRepository.findTotalTaxValues();
        List<Object[]> purchaseDataList = purchaseInvoiceRepository.findTotalItcValues();

        Object[] salesData = null;
        if (salesDataList != null && !salesDataList.isEmpty()) {
            salesData = salesDataList.get(0);
        }

        Object[] purchaseData = null;
        if (purchaseDataList != null && !purchaseDataList.isEmpty()) {
            purchaseData = purchaseDataList.get(0);
        }

        java.math.BigDecimal outwardTaxable = java.math.BigDecimal.ZERO;
        java.math.BigDecimal outwardIgst = java.math.BigDecimal.ZERO;
        java.math.BigDecimal outwardCgst = java.math.BigDecimal.ZERO;
        java.math.BigDecimal outwardSgst = java.math.BigDecimal.ZERO;

        final int indexTaxable = 0;
        final int indexIgst = 1;
        final int indexCgst = 2;
        final int indexSgst = 3;

        if (salesData != null && salesData.length >= indexSgst + 1) {
            if (salesData[indexTaxable] != null) {
                outwardTaxable = new java.math.BigDecimal(salesData[indexTaxable].toString());
            }
            if (salesData[indexIgst] != null) {
                outwardIgst = new java.math.BigDecimal(salesData[indexIgst].toString());
            }
            if (salesData[indexCgst] != null) {
                outwardCgst = new java.math.BigDecimal(salesData[indexCgst].toString());
            }
            if (salesData[indexSgst] != null) {
                outwardSgst = new java.math.BigDecimal(salesData[indexSgst].toString());
            }
        }

        java.math.BigDecimal itcIgst = java.math.BigDecimal.ZERO;
        java.math.BigDecimal itcCgst = java.math.BigDecimal.ZERO;
        java.math.BigDecimal itcSgst = java.math.BigDecimal.ZERO;

        final int indexItcIgst = 0;
        final int indexItcCgst = 1;
        final int indexItcSgst = 2;

        if (purchaseData != null && purchaseData.length >= indexItcSgst + 1) {
            if (purchaseData[indexItcIgst] != null) {
                itcIgst = new java.math.BigDecimal(purchaseData[indexItcIgst].toString());
            }
            if (purchaseData[indexItcCgst] != null) {
                itcCgst = new java.math.BigDecimal(purchaseData[indexItcCgst].toString());
            }
            if (purchaseData[indexItcSgst] != null) {
                itcSgst = new java.math.BigDecimal(purchaseData[indexItcSgst].toString());
            }
        }

        java.math.BigDecimal totalOutputTax = outwardIgst.add(outwardCgst).add(outwardSgst);
        java.math.BigDecimal totalInputTax = itcIgst.add(itcCgst).add(itcSgst);
        java.math.BigDecimal netTaxPayable = totalOutputTax.subtract(totalInputTax);

        return com.finventory.dto.Gstr3bDto.builder()
                .outwardTaxableValue(outwardTaxable)
                .outwardIgst(outwardIgst)
                .outwardCgst(outwardCgst)
                .outwardSgst(outwardSgst)
                .itcIgst(itcIgst)
                .itcCgst(itcCgst)
                .itcSgst(itcSgst)
                .netTaxPayable(netTaxPayable)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedEntryDto> getActivityFeed(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_ACTIVITY_LIMIT));
        PageRequest page = PageRequest.of(0, safeLimit);

        List<ActivityFeedEntryDto> entries = new ArrayList<>();

        var salesInvoices = salesInvoiceRepository.findAllByOrderByInvoiceDateDesc(page);
        for (var inv : salesInvoices) {
            entries.add(
                    ActivityFeedEntryDto.builder()
                            .kind("SALES_INVOICE")
                            .id(inv.getId())
                            .date(inv.getInvoiceDate())
                            .title("Sales Invoice • " + inv.getInvoiceNumber())
                            .subtitle(inv.getParty().getName())
                            .amount(inv.getGrandTotal())
                            .href("/sales/invoices/" + inv.getId())
                            .build());
        }

        var purchaseInvoices = purchaseInvoiceRepository.findAllByOrderByInvoiceDateDesc(page);
        for (var inv : purchaseInvoices) {
            entries.add(
                    ActivityFeedEntryDto.builder()
                            .kind("PURCHASE_INVOICE")
                            .id(inv.getId())
                            .date(inv.getInvoiceDate())
                            .title("Purchase Invoice • " + inv.getInvoiceNumber())
                            .subtitle(inv.getParty().getName())
                            .amount(inv.getGrandTotal())
                            .href("/purchase/invoices/" + inv.getId())
                            .build());
        }

        var salesReturns = salesReturnRepository.findAllByOrderByReturnDateDesc(page);
        for (var ret : salesReturns) {
            entries.add(
                    ActivityFeedEntryDto.builder()
                            .kind("SALES_RETURN")
                            .id(ret.getId())
                            .date(ret.getReturnDate())
                            .title("Sales Return • " + ret.getReturnNumber())
                            .subtitle(ret.getParty().getName())
                            .amount(ret.getGrandTotal())
                            .href("/sales/returns/" + ret.getId())
                            .build());
        }

        var purchaseReturns = purchaseReturnRepository.findAllByOrderByReturnDateDesc(page);
        for (var ret : purchaseReturns) {
            entries.add(
                    ActivityFeedEntryDto.builder()
                            .kind("PURCHASE_RETURN")
                            .id(ret.getId())
                            .date(ret.getReturnDate())
                            .title("Purchase Return • " + ret.getReturnNumber())
                            .subtitle(ret.getParty().getName())
                            .amount(ret.getGrandTotal())
                            .href("/purchase/returns/" + ret.getId())
                            .build());
        }

        var stockAdjustments = stockAdjustmentRepository.findAllByOrderByAdjustmentDateDesc(page);
        for (var adj : stockAdjustments) {
            String qty =
                    (adj.getQuantity() != null && adj.getQuantity().signum() > 0 ? "+" : "")
                            + (adj.getQuantity() != null ? adj.getQuantity().toPlainString() : "0");
            entries.add(
                    ActivityFeedEntryDto.builder()
                            .kind("STOCK_ADJUSTMENT")
                            .id(adj.getId())
                            .date(adj.getAdjustmentDate())
                            .title("Stock Adjustment • " + adj.getAdjustmentNumber())
                            .subtitle(
                                    adj.getWarehouse().getName()
                                            + " • "
                                            + adj.getItem().getName()
                                            + " • "
                                            + qty)
                            .amount(null)
                            .href(null)
                            .build());
        }

        entries.sort(Comparator.comparing(ActivityFeedEntryDto::getDate).reversed());
        return entries.stream().limit(safeLimit).toList();
    }

    @Transactional(readOnly = true)
    public SystemStatusDto getSystemStatus() {
        OffsetDateTime now = OffsetDateTime.now();
        LocalDate today = LocalDate.now();

        try {
            return SystemStatusDto.builder()
                    .app("Finventory Backend")
                    .serverTime(now)
                    .dbUp(true)
                    .dbError(null)
                    .items(itemRepository.count())
                    .parties(partyRepository.count())
                    .warehouses(warehouseRepository.count())
                    .salesInvoices(salesInvoiceRepository.count())
                    .purchaseInvoices(purchaseInvoiceRepository.count())
                    .salesReturns(salesReturnRepository.count())
                    .purchaseReturns(purchaseReturnRepository.count())
                    .stockAdjustments(stockAdjustmentRepository.count())
                    .salesInvoicesToday(salesInvoiceRepository.countByInvoiceDate(today))
                    .purchaseInvoicesToday(purchaseInvoiceRepository.countByInvoiceDate(today))
                    .salesReturnsToday(salesReturnRepository.countByReturnDate(today))
                    .purchaseReturnsToday(purchaseReturnRepository.countByReturnDate(today))
                    .stockAdjustmentsToday(stockAdjustmentRepository.countByAdjustmentDate(today))
                    .build();
        } catch (Exception ex) {
            return SystemStatusDto.builder()
                    .app("Finventory Backend")
                    .serverTime(now)
                    .dbUp(false)
                    .dbError(ex.getMessage())
                    .items(0)
                    .parties(0)
                    .warehouses(0)
                    .salesInvoices(0)
                    .purchaseInvoices(0)
                    .salesReturns(0)
                    .purchaseReturns(0)
                    .stockAdjustments(0)
                    .salesInvoicesToday(0)
                    .purchaseInvoicesToday(0)
                    .salesReturnsToday(0)
                    .purchaseReturnsToday(0)
                    .stockAdjustmentsToday(0)
                    .build();
        }
    }

    public enum ReportInvoiceStatus {
        ACTIVE,
        CANCELLED,
        DELETED,
        ALL
    }

    public enum SalesGroupBy {
        DAY,
        WEEK,
        MONTH,
        RANGE
    }

    public record SalesReportFilter(
            LocalDate fromDate,
            LocalDate toDate,
            SalesGroupBy groupBy,
            UUID itemId,
            String category,
            UUID partyId,
            InvoicePaymentStatus paymentStatus,
            ReportInvoiceStatus invoiceStatus) {}

    @Transactional(readOnly = true)
    public SalesReportDto.Response getSalesReport(SalesReportFilter filter) {
        LocalDate fromDate = filter != null ? filter.fromDate() : null;
        LocalDate toDate = filter != null ? filter.toDate() : null;
        SalesGroupBy groupBy = filter != null ? filter.groupBy() : null;
        UUID itemId = filter != null ? filter.itemId() : null;
        String category = filter != null ? filter.category() : null;
        UUID partyId = filter != null ? filter.partyId() : null;
        InvoicePaymentStatus paymentStatus = filter != null ? filter.paymentStatus() : null;
        ReportInvoiceStatus invoiceStatus = filter != null ? filter.invoiceStatus() : null;
        List<SalesInvoice> invoices =
                salesInvoiceRepository.findForSalesReport(
                        fromDate, toDate, partyId, paymentStatus, itemId, category);

        List<SalesInvoice> filtered = new ArrayList<>();
        for (SalesInvoice s : invoices) {
            if (!matchesInvoiceStatus(s, invoiceStatus)) {
                continue;
            }
            filtered.add(s);
        }

        Map<LocalDate, SalesBucketAgg> bucketsByKey = new LinkedHashMap<>();

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalPending = BigDecimal.ZERO;

        for (SalesInvoice s : filtered) {
            BigDecimal amount = safe(s.getGrandTotal());
            BigDecimal discount = safe(s.getOfferDiscountAmount());
            BigDecimal paid = safe(s.getPaidAmount());
            BigDecimal pending = amount.subtract(paid);

            totalAmount = totalAmount.add(amount);
            totalDiscount = totalDiscount.add(discount);
            totalPaid = totalPaid.add(paid);
            totalPending = totalPending.add(pending);

            LocalDate key =
                    groupBy == SalesGroupBy.RANGE
                            ? (fromDate != null
                                    ? fromDate
                                    : (toDate != null ? toDate : s.getInvoiceDate()))
                            : bucketKey(s.getInvoiceDate(), groupBy);
            var agg =
                    bucketsByKey.computeIfAbsent(
                            key,
                            k ->
                                    new SalesBucketAgg(
                                            groupBy == SalesGroupBy.RANGE
                                                    ? fromDate
                                                    : bucketStart(k, groupBy),
                                            groupBy == SalesGroupBy.RANGE
                                                    ? toDate
                                                    : bucketEnd(k, groupBy),
                                            bucketLabel(k, groupBy, fromDate, toDate)));
            agg.invoiceCount++;
            agg.totalAmount = agg.totalAmount.add(amount);
            agg.totalDiscount = agg.totalDiscount.add(discount);
            agg.totalPaid = agg.totalPaid.add(paid);
            agg.totalPending = agg.totalPending.add(pending);
        }

        List<SalesReportDto.Bucket> buckets = new ArrayList<>();
        for (var e : bucketsByKey.entrySet()) {
            SalesBucketAgg agg = e.getValue();
            buckets.add(
                    SalesReportDto.Bucket.builder()
                            .periodStart(agg.periodStart)
                            .periodEnd(agg.periodEnd)
                            .label(agg.label)
                            .invoiceCount(agg.invoiceCount)
                            .totalAmount(scale(agg.totalAmount))
                            .totalDiscount(scale(agg.totalDiscount))
                            .totalPaid(scale(agg.totalPaid))
                            .totalPending(scale(agg.totalPending))
                            .build());
        }

        return SalesReportDto.Response.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .groupBy(groupBy != null ? groupBy.name() : null)
                .totals(
                        SalesReportDto.Totals.builder()
                                .invoiceCount(filtered.size())
                                .totalAmount(scale(totalAmount))
                                .totalDiscount(scale(totalDiscount))
                                .totalPaid(scale(totalPaid))
                                .totalPending(scale(totalPending))
                                .build())
                .buckets(buckets)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StockReportDto.Row> getStockReport() {
        List<StockSummaryDto> summary = stockLedgerRepository.findStockSummary();
        Map<UUID, BigDecimal> unitPriceByItem = new HashMap<>();
        itemRepository
                .findAll()
                .forEach(
                        i ->
                                unitPriceByItem.put(
                                        i.getId(),
                                        i.getUnitPrice() != null
                                                ? i.getUnitPrice()
                                                : BigDecimal.ZERO));

        List<StockReportDto.Row> rows = new ArrayList<>();
        for (StockSummaryDto s : summary) {
            BigDecimal unitPrice = unitPriceByItem.getOrDefault(s.getItemId(), BigDecimal.ZERO);
            BigDecimal qty = safe(s.getCurrentStock());
            rows.add(
                    StockReportDto.Row.builder()
                            .itemId(s.getItemId())
                            .itemName(s.getItemName())
                            .itemCode(s.getItemCode())
                            .vendorId(s.getVendorId())
                            .vendorName(s.getVendorName())
                            .warehouseId(s.getWarehouseId())
                            .warehouseName(s.getWarehouseName())
                            .currentStock(qty)
                            .uom(s.getUom())
                            .unitPrice(scale(unitPrice))
                            .valuation(scale(unitPrice.multiply(qty)))
                            .build());
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public StockReportDto.LowStockResponse getLowStockReport(BigDecimal threshold) {
        BigDecimal t = threshold != null ? threshold : BigDecimal.ZERO;
        List<StockReportDto.Row> rows = getStockReport();

        List<StockReportDto.Row> low = new ArrayList<>();
        List<StockReportDto.Row> out = new ArrayList<>();
        for (StockReportDto.Row r : rows) {
            BigDecimal qty = safe(r.getCurrentStock());
            if (qty.compareTo(BigDecimal.ZERO) == 0) {
                out.add(r);
            }
            if (qty.compareTo(t) <= 0) {
                low.add(r);
            }
        }

        return StockReportDto.LowStockResponse.builder()
                .threshold(t)
                .lowStock(low)
                .outOfStock(out)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StockReportDto.MovementEntry> getStockMovement(
            LocalDate fromDate, LocalDate toDate, UUID itemId, UUID warehouseId) {
        var entries = stockLedgerRepository.findMovement(fromDate, toDate, itemId, warehouseId);
        List<StockReportDto.MovementEntry> out = new ArrayList<>();
        for (var e : entries) {
            out.add(
                    StockReportDto.MovementEntry.builder()
                            .date(e.getDate())
                            .itemId(e.getItem().getId())
                            .itemName(e.getItem().getName())
                            .warehouseId(e.getWarehouse().getId())
                            .warehouseName(e.getWarehouse().getName())
                            .qtyIn(e.getQtyIn())
                            .qtyOut(e.getQtyOut())
                            .refType(e.getRefType() != null ? e.getRefType().name() : null)
                            .refId(e.getRefId())
                            .build());
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ProfitLossReportDto getProfitLoss(LocalDate fromDate, LocalDate toDate) {
        List<SalesInvoice> invoices =
                salesInvoiceRepository.findForSalesReport(fromDate, toDate, null, null, null, null);
        var salesReturns = salesReturnRepository.findForReport(fromDate, toDate);
        var purchaseInvoices = purchaseInvoiceRepository.findAllWithFilters(null, fromDate, toDate);
        var purchaseReturns = purchaseReturnRepository.findForReport(fromDate, toDate);

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;

        for (SalesInvoice s : invoices) {
            if (!matchesInvoiceStatus(s, ReportInvoiceStatus.ACTIVE)) {
                continue;
            }

            revenue = revenue.add(safe(s.getGrandTotal()));
            discounts = discounts.add(safe(s.getOfferDiscountAmount()));

            for (SalesInvoiceLine l : s.getLines()) {
                BigDecimal qty = safe(l.getQuantity());
                BigDecimal lineCogs =
                        l.getItem() != null ? safe(l.getItem().getCogs()) : BigDecimal.ZERO;
                cogs = cogs.add(qty.multiply(lineCogs));
            }
        }

        for (var r : salesReturns) {
            revenue = revenue.subtract(safe(r.getGrandTotal()));
        }

        for (var p : purchaseInvoices) {
            if (p.getCancelledAt() != null || p.getDeletedAt() != null) {
                continue;
            }
            expenses = expenses.add(safe(p.getGrandTotal()));
        }

        for (var r : purchaseReturns) {
            expenses = expenses.subtract(safe(r.getGrandTotal()));
        }

        BigDecimal grossProfit = revenue.subtract(discounts).subtract(cogs);
        BigDecimal netProfit = grossProfit.subtract(expenses);

        return ProfitLossReportDto.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .revenue(scale(revenue))
                .discounts(scale(discounts))
                .expenses(scale(expenses))
                .cogs(scale(cogs))
                .grossProfit(scale(grossProfit))
                .netProfit(scale(netProfit))
                .build();
    }

    @Transactional(readOnly = true)
    public AnnualReportDto getAnnualReport(int year, int topLimit) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = YearMonth.of(year, Month.DECEMBER).atEndOfMonth();
        List<SalesInvoice> invoices =
                salesInvoiceRepository.findForSalesReport(from, to, null, null, null, null);
        var salesReturns = salesReturnRepository.findForReport(from, to);
        var purchaseInvoices = purchaseInvoiceRepository.findAllWithFilters(null, from, to);
        var purchaseReturns = purchaseReturnRepository.findForReport(from, to);

        Map<YearMonth, AnnualReportDto.MonthTrend.MonthTrendBuilder> months =
                initAnnualMonths(year);
        Map<UUID, AnnualReportDto.TopItem.TopItemBuilder> topItems = new HashMap<>();
        Map<UUID, AnnualReportDto.TopParty.TopPartyBuilder> topCustomers = new HashMap<>();

        applySalesInvoicesToAnnual(months, invoices, topItems, topCustomers);
        applySalesReturnsToAnnual(months, salesReturns);
        applyPurchaseInvoicesToAnnual(months, purchaseInvoices);
        applyPurchaseReturnsToAnnual(months, purchaseReturns);

        List<AnnualReportDto.MonthTrend> monthRows = buildAnnualMonthRows(months);
        List<AnnualReportDto.TopItem> topItemRows = buildAnnualTopItems(topItems, topLimit);
        List<AnnualReportDto.TopParty> topCustomerRows =
                buildAnnualTopCustomers(topCustomers, topLimit);

        return AnnualReportDto.builder()
                .year(year)
                .months(monthRows)
                .topItems(topItemRows)
                .topCustomers(topCustomerRows)
                .build();
    }

    private static Map<YearMonth, AnnualReportDto.MonthTrend.MonthTrendBuilder> initAnnualMonths(
            int year) {
        Map<YearMonth, AnnualReportDto.MonthTrend.MonthTrendBuilder> months = new LinkedHashMap<>();
        for (int m = 1; m <= MONTHS_IN_YEAR; m++) {
            YearMonth ym = YearMonth.of(year, m);
            months.put(
                    ym,
                    AnnualReportDto.MonthTrend.builder()
                            .month(m)
                            .revenue(BigDecimal.ZERO)
                            .discounts(BigDecimal.ZERO)
                            .expenses(BigDecimal.ZERO)
                            .cogs(BigDecimal.ZERO)
                            .grossProfit(BigDecimal.ZERO)
                            .netProfit(BigDecimal.ZERO));
        }
        return months;
    }

    private void applySalesInvoicesToAnnual(
            Map<YearMonth, AnnualReportDto.MonthTrend.MonthTrendBuilder> months,
            List<SalesInvoice> invoices,
            Map<UUID, AnnualReportDto.TopItem.TopItemBuilder> topItems,
            Map<UUID, AnnualReportDto.TopParty.TopPartyBuilder> topCustomers) {
        for (SalesInvoice s : invoices) {
            if (!matchesInvoiceStatus(s, ReportInvoiceStatus.ACTIVE)) {
                continue;
            }

            YearMonth ym = YearMonth.from(s.getInvoiceDate());
            var mt = months.get(ym);
            if (mt == null) {
                continue;
            }

            BigDecimal amount = safe(s.getGrandTotal());
            BigDecimal discount = safe(s.getOfferDiscountAmount());
            BigDecimal invCogs = BigDecimal.ZERO;
            for (SalesInvoiceLine l : s.getLines()) {
                BigDecimal qty = safe(l.getQuantity());
                BigDecimal lineCogs =
                        l.getItem() != null ? safe(l.getItem().getCogs()) : BigDecimal.ZERO;
                invCogs = invCogs.add(qty.multiply(lineCogs));

                if (l.getItem() != null) {
                    UUID itemKey = l.getItem().getId();
                    var it =
                            topItems.computeIfAbsent(
                                    itemKey,
                                    k ->
                                            AnnualReportDto.TopItem.builder()
                                                    .itemId(itemKey)
                                                    .itemName(l.getItem().getName())
                                                    .itemCode(l.getItem().getCode())
                                                    .quantity(BigDecimal.ZERO)
                                                    .amount(BigDecimal.ZERO));
                    it.quantity(it.build().getQuantity().add(qty));
                    it.amount(it.build().getAmount().add(safe(l.getLineTotal())));
                }
            }

            mt.revenue(mt.build().getRevenue().add(amount));
            mt.discounts(mt.build().getDiscounts().add(discount));
            mt.cogs(mt.build().getCogs().add(invCogs));
            mt.grossProfit(
                    mt.build().getGrossProfit().add(amount.subtract(discount).subtract(invCogs)));

            if (s.getParty() != null) {
                UUID partyKey = s.getParty().getId();
                var pt =
                        topCustomers.computeIfAbsent(
                                partyKey,
                                k ->
                                        AnnualReportDto.TopParty.builder()
                                                .partyId(partyKey)
                                                .partyName(s.getParty().getName())
                                                .amount(BigDecimal.ZERO)
                                                .invoiceCount(0L));
                pt.amount(pt.build().getAmount().add(amount));
                pt.invoiceCount(pt.build().getInvoiceCount() + 1);
            }
        }
    }

    private void applySalesReturnsToAnnual(
            Map<YearMonth, AnnualReportDto.MonthTrend.MonthTrendBuilder> months,
            List<com.finventory.model.SalesReturn> salesReturns) {
        for (var r : salesReturns) {
            YearMonth ym = YearMonth.from(r.getReturnDate());
            var mt = months.get(ym);
            if (mt == null) {
                continue;
            }
            BigDecimal amount = safe(r.getGrandTotal());
            mt.revenue(mt.build().getRevenue().subtract(amount));
            mt.grossProfit(mt.build().getGrossProfit().subtract(amount));
        }
    }

    private void applyPurchaseInvoicesToAnnual(
            Map<YearMonth, AnnualReportDto.MonthTrend.MonthTrendBuilder> months,
            List<com.finventory.model.PurchaseInvoice> purchaseInvoices) {
        for (var p : purchaseInvoices) {
            if (p.getCancelledAt() != null || p.getDeletedAt() != null) {
                continue;
            }
            YearMonth ym = YearMonth.from(p.getInvoiceDate());
            var mt = months.get(ym);
            if (mt == null) {
                continue;
            }
            mt.expenses(mt.build().getExpenses().add(safe(p.getGrandTotal())));
        }
    }

    private void applyPurchaseReturnsToAnnual(
            Map<YearMonth, AnnualReportDto.MonthTrend.MonthTrendBuilder> months,
            List<com.finventory.model.PurchaseReturn> purchaseReturns) {
        for (var r : purchaseReturns) {
            YearMonth ym = YearMonth.from(r.getReturnDate());
            var mt = months.get(ym);
            if (mt == null) {
                continue;
            }
            mt.expenses(mt.build().getExpenses().subtract(safe(r.getGrandTotal())));
        }
    }

    private static List<AnnualReportDto.MonthTrend> buildAnnualMonthRows(
            Map<YearMonth, AnnualReportDto.MonthTrend.MonthTrendBuilder> months) {
        List<AnnualReportDto.MonthTrend> monthRows = new ArrayList<>();
        for (var b : months.values()) {
            var built = b.build();
            monthRows.add(
                    AnnualReportDto.MonthTrend.builder()
                            .month(built.getMonth())
                            .revenue(scale(built.getRevenue()))
                            .discounts(scale(built.getDiscounts()))
                            .expenses(scale(built.getExpenses()))
                            .cogs(scale(built.getCogs()))
                            .grossProfit(scale(built.getGrossProfit()))
                            .netProfit(
                                    scale(
                                            built.getGrossProfit()
                                                    .subtract(safe(built.getExpenses()))))
                            .build());
        }
        return monthRows;
    }

    private static List<AnnualReportDto.TopItem> buildAnnualTopItems(
            Map<UUID, AnnualReportDto.TopItem.TopItemBuilder> topItems, int topLimit) {
        return topItems.values().stream()
                .map(
                        b ->
                                AnnualReportDto.TopItem.builder()
                                        .itemId(b.build().getItemId())
                                        .itemName(b.build().getItemName())
                                        .itemCode(b.build().getItemCode())
                                        .quantity(scale(b.build().getQuantity()))
                                        .amount(scale(b.build().getAmount()))
                                        .build())
                .sorted((a, c) -> safe(c.getAmount()).compareTo(safe(a.getAmount())))
                .limit(Math.max(0, topLimit))
                .toList();
    }

    private static List<AnnualReportDto.TopParty> buildAnnualTopCustomers(
            Map<UUID, AnnualReportDto.TopParty.TopPartyBuilder> topCustomers, int topLimit) {
        return topCustomers.values().stream()
                .map(
                        b ->
                                AnnualReportDto.TopParty.builder()
                                        .partyId(b.build().getPartyId())
                                        .partyName(b.build().getPartyName())
                                        .amount(scale(b.build().getAmount()))
                                        .invoiceCount(b.build().getInvoiceCount())
                                        .build())
                .sorted((a, c) -> safe(c.getAmount()).compareTo(safe(a.getAmount())))
                .limit(Math.max(0, topLimit))
                .toList();
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal scale(BigDecimal v) {
        return safe(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean matchesInvoiceStatus(SalesInvoice s, ReportInvoiceStatus status) {
        if (status == null || status == ReportInvoiceStatus.ALL) {
            return true;
        }
        return switch (status) {
            case ACTIVE -> s.getCancelledAt() == null && s.getDeletedAt() == null;
            case CANCELLED -> s.getCancelledAt() != null;
            case DELETED -> s.getDeletedAt() != null;
            case ALL -> true;
        };
    }

    private static final class SalesBucketAgg {
        private final LocalDate periodStart;
        private final LocalDate periodEnd;
        private final String label;
        private long invoiceCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal totalDiscount = BigDecimal.ZERO;
        private BigDecimal totalPaid = BigDecimal.ZERO;
        private BigDecimal totalPending = BigDecimal.ZERO;

        private SalesBucketAgg(LocalDate periodStart, LocalDate periodEnd, String label) {
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
            this.label = label;
        }
    }

    private static LocalDate bucketKey(LocalDate date, SalesGroupBy groupBy) {
        if (date == null) {
            return null;
        }
        SalesGroupBy g = groupBy != null ? groupBy : SalesGroupBy.DAY;
        return switch (g) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
            case RANGE -> date;
        };
    }

    private static LocalDate bucketStart(LocalDate key, SalesGroupBy groupBy) {
        if (key == null) {
            return null;
        }
        SalesGroupBy g = groupBy != null ? groupBy : SalesGroupBy.DAY;
        return switch (g) {
            case DAY, WEEK, MONTH, RANGE -> key;
        };
    }

    private static LocalDate bucketEnd(LocalDate key, SalesGroupBy groupBy) {
        if (key == null) {
            return null;
        }
        SalesGroupBy g = groupBy != null ? groupBy : SalesGroupBy.DAY;
        return switch (g) {
            case DAY -> key;
            case WEEK -> key.plusDays(DAYS_IN_WEEK_MINUS_ONE);
            case MONTH -> YearMonth.from(key).atEndOfMonth();
            case RANGE -> key;
        };
    }

    private static String bucketLabel(
            LocalDate key, SalesGroupBy groupBy, LocalDate fromDate, LocalDate toDate) {
        if (key == null) {
            return null;
        }
        SalesGroupBy g = groupBy != null ? groupBy : SalesGroupBy.DAY;
        if (g == SalesGroupBy.RANGE) {
            return (fromDate != null ? fromDate.toString() : "")
                    + " → "
                    + (toDate != null ? toDate.toString() : "");
        }
        if (g == SalesGroupBy.WEEK) {
            var wf = WeekFields.ISO;
            int week = key.get(wf.weekOfWeekBasedYear());
            int weekYear = key.get(wf.weekBasedYear());
            return weekYear + "-W" + String.format("%02d", week);
        }
        if (g == SalesGroupBy.MONTH) {
            YearMonth ym = YearMonth.from(key);
            return ym.toString();
        }
        return key.toString();
    }
}
