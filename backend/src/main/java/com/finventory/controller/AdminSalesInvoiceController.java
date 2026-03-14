package com.finventory.controller;

import com.finventory.service.SalesInvoiceService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sales-invoices")
@RequiredArgsConstructor
public class AdminSalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;

    @PostMapping("/apply-standard-discount")
    public ResponseEntity<ApplyStandardDiscountResponse> applyStandardDiscount(
            @RequestParam(name = "percent", defaultValue = "10") BigDecimal percent) {
        int updated = salesInvoiceService.applyStandardDiscountToAllSalesInvoices(percent);
        return ResponseEntity.ok(new ApplyStandardDiscountResponse(updated));
    }

    public record ApplyStandardDiscountResponse(int updatedCount) {}
}
