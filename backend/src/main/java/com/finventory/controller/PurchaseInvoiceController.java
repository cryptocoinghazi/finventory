package com.finventory.controller;

import com.finventory.dto.PurchaseInvoiceDto;
import com.finventory.service.PurchaseInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-invoices")
@RequiredArgsConstructor
public class PurchaseInvoiceController {

  private final PurchaseInvoiceService purchaseInvoiceService;

  @PostMapping
  public ResponseEntity<PurchaseInvoiceDto> createPurchaseInvoice(
      @Valid @RequestBody PurchaseInvoiceDto dto) {
    PurchaseInvoiceDto created = purchaseInvoiceService.createPurchaseInvoice(dto);
    return new ResponseEntity<>(created, HttpStatus.CREATED);
  }
}
