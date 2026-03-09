package com.finventory.service;

import com.finventory.dto.StockAdjustmentDto;
import com.finventory.model.Item;
import com.finventory.model.SequenceType;
import com.finventory.model.StockAdjustment;
import com.finventory.model.StockLedgerEntry;
import com.finventory.model.Warehouse;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.StockAdjustmentRepository;
import com.finventory.repository.WarehouseRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockAdjustmentService {

    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final StockPostingService stockPostingService;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional
    public StockAdjustmentDto createAdjustment(StockAdjustmentDto dto) {
        Warehouse warehouse =
                warehouseRepository
                        .findById(dto.getWarehouseId())
                        .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
        Item item =
                itemRepository
                        .findById(dto.getItemId())
                        .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        String adjustmentNumber =
                sequenceGeneratorService.generateSequence(
                        SequenceType.STOCK_ADJUSTMENT, warehouse, dto.getAdjustmentDate());

        StockAdjustment adjustment =
                StockAdjustment.builder()
                        .adjustmentNumber(adjustmentNumber)
                        .adjustmentDate(dto.getAdjustmentDate())
                        .warehouse(warehouse)
                        .item(item)
                        .quantity(dto.getQuantity())
                        .reason(dto.getReason())
                        .build();

        StockAdjustment saved = stockAdjustmentRepository.save(adjustment);

        // Post to Stock Ledger
        if (dto.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            stockPostingService.postStockIn(
                    saved.getAdjustmentDate(),
                    item,
                    warehouse,
                    saved.getQuantity(),
                    StockLedgerEntry.ReferenceType.STOCK_ADJUSTMENT,
                    saved.getId());
        } else if (dto.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            stockPostingService.postStockOut(
                    saved.getAdjustmentDate(),
                    item,
                    warehouse,
                    saved.getQuantity().abs(),
                    StockLedgerEntry.ReferenceType.STOCK_ADJUSTMENT,
                    saved.getId());
        }

        return mapToDto(saved);
    }

    private StockAdjustmentDto mapToDto(StockAdjustment entity) {
        return StockAdjustmentDto.builder()
                .id(entity.getId())
                .adjustmentNumber(entity.getAdjustmentNumber())
                .adjustmentDate(entity.getAdjustmentDate())
                .warehouseId(entity.getWarehouse().getId())
                .warehouseName(entity.getWarehouse().getName())
                .itemId(entity.getItem().getId())
                .itemName(entity.getItem().getName())
                .quantity(entity.getQuantity())
                .reason(entity.getReason())
                .build();
    }
}
