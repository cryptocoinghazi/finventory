package com.finventory.repository;

import com.finventory.dto.PartyOutstandingDto;
import com.finventory.model.GLTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GLTransactionRepository extends JpaRepository<GLTransaction, UUID> {

  // We need to join with GLLines to filter by account head (AR/AP)
  // For simplicity, let's assume net balance (Debit - Credit) per party is the outstanding.
  // Positive Net Balance = Receivable (Asset)
  // Negative Net Balance = Payable (Liability)
  @Query("SELECT new com.finventory.dto.PartyOutstandingDto("
      + "t.party.id, t.party.name, CAST(t.party.type AS string), "
      + "SUM(l.debit), SUM(l.credit), SUM(l.debit - l.credit)) "
      + "FROM GLTransaction t JOIN t.lines l "
      + "WHERE t.party IS NOT NULL "
      + "AND (l.accountHead = 'ACCOUNTS_RECEIVABLE' OR l.accountHead = 'ACCOUNTS_PAYABLE') "
      + "GROUP BY t.party.id, t.party.name, t.party.type")
  List<PartyOutstandingDto> findPartyOutstanding();
}
