package com.finventory.controller;

import com.finventory.dto.ActivityFeedEntryDto;
import com.finventory.dto.PartyLedgerEntryDto;
import com.finventory.dto.PartyOutstandingDto;
import com.finventory.dto.StockSummaryDto;
import com.finventory.dto.SystemStatusDto;
import com.finventory.service.ReportsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<com.finventory.dto.DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(reportsService.getDashboardStats());
    }

    @GetMapping("/stock-summary")
    public ResponseEntity<List<StockSummaryDto>> getStockSummary() {
        return ResponseEntity.ok(reportsService.getStockSummary());
    }

    @GetMapping("/party-outstanding")
    public ResponseEntity<List<PartyOutstandingDto>> getPartyOutstanding(
            @RequestParam(name = "fromDate", required = false) String fromDate,
            @RequestParam(name = "toDate", required = false) String toDate,
            @RequestParam(name = "asOfDate", required = false) String asOfDate,
            @RequestParam(name = "partyType", required = false) String partyType,
            @RequestParam(name = "minOutstanding", required = false) BigDecimal minOutstanding) {
        LocalDate parsedFromDate = parseDate(fromDate, "fromDate");
        LocalDate parsedToDate = parseDate(toDate, "toDate");
        LocalDate parsedAsOfDate = parseDate(asOfDate, "asOfDate");
        com.finventory.model.Party.PartyType parsedPartyType = parsePartyType(partyType);

        return ResponseEntity.ok(
                reportsService.getPartyOutstanding(
                        parsedFromDate,
                        parsedToDate,
                        parsedPartyType,
                        minOutstanding,
                        parsedAsOfDate));
    }

    @GetMapping("/party-outstanding/ledger")
    public ResponseEntity<List<PartyLedgerEntryDto>> getPartyOutstandingLedger(
            @RequestParam(name = "partyId") UUID partyId,
            @RequestParam(name = "fromDate", required = false) String fromDate,
            @RequestParam(name = "toDate", required = false) String toDate) {
        LocalDate parsedFromDate = parseDate(fromDate, "fromDate");
        LocalDate parsedToDate = parseDate(toDate, "toDate");
        return ResponseEntity.ok(
                reportsService.getPartyOutstandingLedger(partyId, parsedFromDate, parsedToDate));
    }

    private static LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be ISO date (yyyy-MM-dd)");
        }
    }

    private static com.finventory.model.Party.PartyType parsePartyType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return com.finventory.model.Party.PartyType.valueOf(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("partyType must be CUSTOMER or VENDOR");
        }
    }

    @GetMapping("/gstr-1")
    public ResponseEntity<List<com.finventory.dto.GstRegisterEntryDto>> getGstr1() {
        return ResponseEntity.ok(reportsService.getGstr1());
    }

    @GetMapping("/gstr-2")
    public ResponseEntity<List<com.finventory.dto.GstRegisterEntryDto>> getGstr2() {
        return ResponseEntity.ok(reportsService.getGstr2());
    }

    @GetMapping("/gstr-3b")
    public ResponseEntity<com.finventory.dto.Gstr3bDto> getGstr3b() {
        return ResponseEntity.ok(reportsService.getGstr3b());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<ActivityFeedEntryDto>> getActivityFeed(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportsService.getActivityFeed(limit));
    }

    @GetMapping("/system-status")
    public ResponseEntity<SystemStatusDto> getSystemStatus() {
        return ResponseEntity.ok(reportsService.getSystemStatus());
    }
}
