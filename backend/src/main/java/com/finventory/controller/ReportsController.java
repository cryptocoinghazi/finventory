package com.finventory.controller;

import com.finventory.dto.PartyOutstandingDto;
import com.finventory.dto.StockSummaryDto;
import com.finventory.service.ReportsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

  private final ReportsService reportsService;

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
}
