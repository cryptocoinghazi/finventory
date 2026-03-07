package com.finventory.controller;

import com.finventory.dto.SalesInvoiceDto;
import com.finventory.service.SalesInvoiceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales-invoices")
@RequiredArgsConstructor
public class SalesInvoiceController {

  private final SalesInvoiceService salesInvoiceService;

  @PostMapping
  public ResponseEntity<SalesInvoiceDto> createSalesInvoice(@Valid @RequestBody SalesInvoiceDto dto) {
    return ResponseEntity.ok(salesInvoiceService.createSalesInvoice(dto));
  }

  @GetMapping
  public ResponseEntity<List<SalesInvoiceDto>> getAllSalesInvoices() {
    return ResponseEntity.ok(salesInvoiceService.getAllSalesInvoices());
  }

  @GetMapping("/{id}")
  public ResponseEntity<SalesInvoiceDto> getSalesInvoice(@PathVariable UUID id) {
    return ResponseEntity.ok(salesInvoiceService.getSalesInvoice(id));
  }
}
