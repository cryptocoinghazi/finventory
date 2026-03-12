package com.finventory.service;

import com.finventory.dto.ActivityFeedEntryDto;
import com.finventory.dto.PartyLedgerEntryDto;
import com.finventory.dto.PartyOutstandingDto;
import com.finventory.dto.StockSummaryDto;
import com.finventory.dto.SystemStatusDto;
import com.finventory.model.GLTransaction;
import com.finventory.model.Party;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
}
