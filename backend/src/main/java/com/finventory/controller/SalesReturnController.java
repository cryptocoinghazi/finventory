package com.finventory.controller;

import com.finventory.dto.SalesReturnDto;
import com.finventory.service.SalesReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales-returns")
@RequiredArgsConstructor
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    @PostMapping
    public ResponseEntity<SalesReturnDto> createSalesReturn(
            @Valid @RequestBody SalesReturnDto dto) {
        SalesReturnDto created = salesReturnService.createSalesReturn(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<java.util.List<SalesReturnDto>> getAllSalesReturns() {
        return ResponseEntity.ok(salesReturnService.getAllSalesReturns());
    }
}
