package com.finventory.controller;

import com.finventory.dto.PurchaseReturnDto;
import com.finventory.service.PurchaseReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-returns")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @PostMapping
    public ResponseEntity<PurchaseReturnDto> createPurchaseReturn(
            @Valid @RequestBody PurchaseReturnDto dto) {
        PurchaseReturnDto created = purchaseReturnService.createPurchaseReturn(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<java.util.List<PurchaseReturnDto>> getAllPurchaseReturns() {
        return ResponseEntity.ok(purchaseReturnService.getAllPurchaseReturns());
    }
}
