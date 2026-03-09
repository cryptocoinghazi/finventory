package com.finventory.controller;

import com.finventory.dto.WarehouseDto;
import com.finventory.service.WarehouseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<WarehouseDto> createWarehouse(
            @Valid @RequestBody WarehouseDto warehouseDto) {
        return ResponseEntity.ok(warehouseService.createWarehouse(warehouseDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseDto> updateWarehouse(
            @PathVariable UUID id, @Valid @RequestBody WarehouseDto warehouseDto) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, warehouseDto));
    }

    @GetMapping
    public ResponseEntity<List<WarehouseDto>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDto> getWarehouseById(@PathVariable UUID id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable UUID id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }
}
