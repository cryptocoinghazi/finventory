package com.finventory.repository;

import com.finventory.dto.GstRegisterEntryDto;
import com.finventory.model.PurchaseInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, UUID> {
    boolean existsByInvoiceNumber(String invoiceNumber);

    List<PurchaseInvoice> findAllByOrderByInvoiceDateDesc(Pageable pageable);

    long countByInvoiceDate(LocalDate invoiceDate);

    @Query(
            "SELECT new com.finventory.dto.GstRegisterEntryDto("
                    + "p.invoiceNumber, p.invoiceDate, p.party.name, p.party.gstin, "
                    + "p.party.stateCode, CAST(p.party.type AS string), "
                    + "p.totalTaxableAmount, p.totalCgstAmount, p.totalSgstAmount, p.totalIgstAmount, p.grandTotal) "
                    + "FROM PurchaseInvoice p")
    List<GstRegisterEntryDto> findGstr2Data();

    @Query(
            "SELECT SUM(p.totalIgstAmount), SUM(p.totalCgstAmount), SUM(p.totalSgstAmount) "
                    + "FROM PurchaseInvoice p")
    List<Object[]> findTotalItcValues();

    @Query("SELECT SUM(p.grandTotal) FROM PurchaseInvoice p WHERE p.invoiceDate = CURRENT_DATE")
    java.math.BigDecimal findTotalPurchaseToday();
}
