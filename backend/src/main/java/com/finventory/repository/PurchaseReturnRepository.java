package com.finventory.repository;

import com.finventory.model.PurchaseReturn;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, UUID> {
    List<PurchaseReturn> findByPurchaseInvoiceId(UUID purchaseInvoiceId);

    @Query(
            "SELECT r FROM PurchaseReturn r "
                    + "WHERE r.returnDate >= COALESCE(:fromDate, r.returnDate) "
                    + "AND r.returnDate <= COALESCE(:toDate, r.returnDate) "
                    + "ORDER BY r.returnDate ASC")
    List<PurchaseReturn> findForReport(
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    void deleteByPartyId(UUID partyId);

    List<PurchaseReturn> findAllByPartyId(UUID partyId);

    List<PurchaseReturn> findAllByOrderByReturnDateDesc(Pageable pageable);

    long countByReturnDate(LocalDate returnDate);
}
