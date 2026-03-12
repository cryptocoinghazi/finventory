package com.finventory.repository;

import com.finventory.model.SalesReturn;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, UUID> {
    List<SalesReturn> findBySalesInvoiceId(UUID salesInvoiceId);

    void deleteByPartyId(UUID partyId);

    List<SalesReturn> findAllByPartyId(UUID partyId);

    List<SalesReturn> findAllByOrderByReturnDateDesc(Pageable pageable);

    long countByReturnDate(LocalDate returnDate);
}
