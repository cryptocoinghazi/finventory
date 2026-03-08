package com.finventory.repository;

import com.finventory.dto.StockSummaryDto;
import com.finventory.model.StockLedgerEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedgerEntry, UUID> {

  @Query("SELECT new com.finventory.dto.StockSummaryDto("
      + "e.item.id, e.item.name, e.item.code, "
      + "e.warehouse.id, e.warehouse.name, "
      + "SUM(e.qtyIn - e.qtyOut), e.item.uom) "
      + "FROM StockLedgerEntry e "
      + "GROUP BY e.item.id, e.item.name, e.item.code, "
      + "e.warehouse.id, e.warehouse.name, e.item.uom")
  List<StockSummaryDto> findStockSummary();
}
