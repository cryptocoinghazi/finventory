package com.finventory.controller;

import com.finventory.dto.StockAdjustmentDto;
import com.finventory.service.StockAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock-adjustments")
@RequiredArgsConstructor
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;

    @PostMapping
    public ResponseEntity<StockAdjustmentDto> createAdjustment(
            @Valid @RequestBody StockAdjustmentDto dto) {
        return ResponseEntity.ok(stockAdjustmentService.createAdjustment(dto));
    }
}
