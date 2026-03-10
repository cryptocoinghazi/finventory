package com.finventory.controller;

import com.finventory.dto.ActivityFeedEntryDto;
import com.finventory.dto.PartyOutstandingDto;
import com.finventory.dto.StockSummaryDto;
import com.finventory.dto.SystemStatusDto;
import com.finventory.service.ReportsService;
import java.util.List;
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
    public ResponseEntity<List<PartyOutstandingDto>> getPartyOutstanding() {
        return ResponseEntity.ok(reportsService.getPartyOutstanding());
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
