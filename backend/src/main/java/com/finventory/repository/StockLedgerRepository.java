package com.finventory.repository;

import com.finventory.model.StockLedgerEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedgerEntry, UUID> {
}
