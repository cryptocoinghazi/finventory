package com.finventory.service;

import com.finventory.model.Item;
import com.finventory.model.StockLedgerEntry;
import com.finventory.model.Warehouse;
import com.finventory.repository.StockLedgerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockPostingService {

    private final StockLedgerRepository stockLedgerRepository;

    @Transactional
    public void postStockOut(
            LocalDate date,
            Item item,
            Warehouse warehouse,
            BigDecimal quantity,
            StockLedgerEntry.ReferenceType refType,
            UUID refId) {
        StockLedgerEntry entry =
                StockLedgerEntry.builder()
                        .date(date)
                        .item(item)
                        .warehouse(warehouse)
                        .qtyIn(BigDecimal.ZERO)
                        .qtyOut(quantity)
                        .refType(refType)
                        .refId(refId)
                        .build();

        stockLedgerRepository.save(entry);
    }

    @Transactional
    public void postStockIn(
            LocalDate date,
            Item item,
            Warehouse warehouse,
            BigDecimal quantity,
            StockLedgerEntry.ReferenceType refType,
            UUID refId) {
        StockLedgerEntry entry =
                StockLedgerEntry.builder()
                        .date(date)
                        .item(item)
                        .warehouse(warehouse)
                        .qtyIn(quantity)
                        .qtyOut(BigDecimal.ZERO)
                        .refType(refType)
                        .refId(refId)
                        .build();

        stockLedgerRepository.save(entry);
    }
}
