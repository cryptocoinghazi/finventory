package com.finventory.repository;

import com.finventory.model.SalesReturn;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, UUID> {
    List<SalesReturn> findBySalesInvoiceId(UUID salesInvoiceId);

    @Query(
            "SELECT r FROM SalesReturn r "
                    + "WHERE r.returnDate >= COALESCE(:fromDate, r.returnDate) "
                    + "AND r.returnDate <= COALESCE(:toDate, r.returnDate) "
                    + "ORDER BY r.returnDate ASC")
    List<SalesReturn> findForReport(
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    void deleteByPartyId(UUID partyId);

    List<SalesReturn> findAllByPartyId(UUID partyId);

    List<SalesReturn> findAllByOrderByReturnDateDesc(Pageable pageable);

    long countByReturnDate(LocalDate returnDate);
}
