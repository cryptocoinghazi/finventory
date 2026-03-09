package com.finventory.repository;

import com.finventory.model.GLLine;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GLLineRepository extends JpaRepository<GLLine, UUID> {
    @Query("SELECT SUM(l.debit - l.credit) FROM GLLine l WHERE l.accountHead = 'ACCOUNTS_RECEIVABLE'")
    BigDecimal findTotalReceivables();
}
