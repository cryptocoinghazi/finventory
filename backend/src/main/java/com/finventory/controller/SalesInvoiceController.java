package com.finventory.controller;

import com.finventory.dto.SalesInvoiceDto;
import com.finventory.model.InvoicePaymentStatus;
import com.finventory.service.SalesInvoiceService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/sales-invoices")
@RequiredArgsConstructor
public class SalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;

    @PostMapping
    public ResponseEntity<SalesInvoiceDto> createSalesInvoice(
            @Valid @RequestBody SalesInvoiceDto dto) {
        return ResponseEntity.ok(salesInvoiceService.createSalesInvoice(dto));
    }

    @GetMapping
    public ResponseEntity<List<SalesInvoiceDto>> getAllSalesInvoices(
            @RequestParam(required = false) InvoicePaymentStatus paymentStatus,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        return ResponseEntity.ok(
                salesInvoiceService.getAllSalesInvoices(paymentStatus, fromDate, toDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesInvoiceDto> getSalesInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(salesInvoiceService.getSalesInvoice(id));
    }

    @PutMapping("/{id}/payment-status")
    public ResponseEntity<SalesInvoiceDto> updatePaymentStatus(
            @PathVariable UUID id, @RequestBody PaymentStatusUpdateRequest request) {
        return ResponseEntity.ok(
                salesInvoiceService.updatePaymentStatus(id, request.getPaymentStatus()));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<SalesInvoiceDto> applyPayment(
            @PathVariable UUID id, @RequestBody PaymentUpdateRequest request) {
        return ResponseEntity.ok(
                salesInvoiceService.applyPayment(
                        id, request.getPaymentStatus(), request.getPaymentAmount()));
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

    public static class PaymentUpdateRequest {
        private InvoicePaymentStatus paymentStatus;
        private BigDecimal paymentAmount;

        public InvoicePaymentStatus getPaymentStatus() {
            return paymentStatus;
        }

        public void setPaymentStatus(InvoicePaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
        }

        public BigDecimal getPaymentAmount() {
            return paymentAmount;
        }

        public void setPaymentAmount(BigDecimal paymentAmount) {
            this.paymentAmount = paymentAmount;
        }
    }
}
