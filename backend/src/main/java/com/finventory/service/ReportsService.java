package com.finventory.service;

import com.finventory.dto.PartyOutstandingDto;
import com.finventory.dto.StockSummaryDto;
import com.finventory.repository.GLTransactionRepository;
import com.finventory.repository.StockLedgerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportsService {

    private final StockLedgerRepository stockLedgerRepository;
    private final GLTransactionRepository glTransactionRepository;
    private final com.finventory.repository.GLLineRepository glLineRepository;
    private final com.finventory.repository.SalesInvoiceRepository salesInvoiceRepository;
    private final com.finventory.repository.PurchaseInvoiceRepository purchaseInvoiceRepository;

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
        return glTransactionRepository.findPartyOutstanding();
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
}
