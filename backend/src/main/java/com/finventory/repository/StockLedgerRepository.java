package com.finventory.repository;

import com.finventory.dto.StockSummaryDto;
import com.finventory.model.StockLedgerEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedgerEntry, UUID> {

    @Query(
            "SELECT new com.finventory.dto.StockSummaryDto("
                    + "e.item.id, e.item.name, e.item.code, "
                    + "v.id, v.name, "
                    + "e.warehouse.id, e.warehouse.name, "
                    + "SUM(e.qtyIn - e.qtyOut), e.item.uom) "
                    + "FROM StockLedgerEntry e "
                    + "LEFT JOIN e.item.vendor v "
                    + "GROUP BY e.item.id, e.item.name, e.item.code, "
                    + "v.id, v.name, "
                    + "e.warehouse.id, e.warehouse.name, e.item.uom")
    List<StockSummaryDto> findStockSummary();

    @Query("SELECT SUM((e.qtyIn - e.qtyOut) * e.item.unitPrice) FROM StockLedgerEntry e")
    java.math.BigDecimal findTotalStockValue();

    @Query(
            "SELECT e FROM StockLedgerEntry e "
                    + "JOIN FETCH e.item i "
                    + "JOIN FETCH e.warehouse w "
                    + "WHERE (:fromDate IS NULL OR e.date >= :fromDate) "
                    + "AND (:toDate IS NULL OR e.date <= :toDate) "
                    + "AND (:itemId IS NULL OR i.id = :itemId) "
                    + "AND (:warehouseId IS NULL OR w.id = :warehouseId) "
                    + "ORDER BY e.date DESC")
    List<StockLedgerEntry> findMovement(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("itemId") UUID itemId,
            @Param("warehouseId") UUID warehouseId);
}
