package com.finventory.controller;

import com.finventory.dto.PurchaseInvoiceDto;
import com.finventory.model.InvoicePaymentStatus;
import com.finventory.service.PurchaseInvoiceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-invoices")
@RequiredArgsConstructor
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService purchaseInvoiceService;

    @GetMapping
    public ResponseEntity<List<PurchaseInvoiceDto>> getAllPurchaseInvoices(
            @RequestParam(required = false) InvoicePaymentStatus paymentStatus,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(
                purchaseInvoiceService.getAllPurchaseInvoices(paymentStatus, fromDate, toDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseInvoiceDto> getPurchaseInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseInvoiceService.getPurchaseInvoice(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseInvoiceDto> createPurchaseInvoice(
            @Valid @RequestBody PurchaseInvoiceDto dto) {
        PurchaseInvoiceDto created = purchaseInvoiceService.createPurchaseInvoice(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/payment-status")
    public ResponseEntity<PurchaseInvoiceDto> updatePaymentStatus(
            @PathVariable UUID id, @RequestBody PaymentStatusUpdateRequest request) {
        PurchaseInvoiceDto updated =
                purchaseInvoiceService.updatePaymentStatus(id, request.getPaymentStatus());
        return ResponseEntity.ok(updated);
    }

    public static class PaymentStatusUpdateRequest {
        private InvoicePaymentStatus paymentStatus;

        public InvoicePaymentStatus getPaymentStatus() {
            return paymentStatus;
        }

        public void setPaymentStatus(InvoicePaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
        }
    }
}
