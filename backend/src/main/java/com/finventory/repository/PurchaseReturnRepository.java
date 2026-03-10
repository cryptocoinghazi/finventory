package com.finventory.repository;

import com.finventory.model.PurchaseReturn;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, UUID> {
    List<PurchaseReturn> findByPurchaseInvoiceId(UUID purchaseInvoiceId);

    List<PurchaseReturn> findAllByOrderByReturnDateDesc(Pageable pageable);

    long countByReturnDate(LocalDate returnDate);
}
