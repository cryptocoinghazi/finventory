package com.finventory.repository;

import com.finventory.dto.GstRegisterEntryDto;
import com.finventory.model.SalesInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, UUID> {
    Optional<SalesInvoice> findByInvoiceNumber(String invoiceNumber);

    List<SalesInvoice> findAllByOrderByInvoiceDateDesc(Pageable pageable);

    long countByInvoiceDate(LocalDate invoiceDate);

    @Query(
            "SELECT new com.finventory.dto.GstRegisterEntryDto("
                    + "s.invoiceNumber, s.invoiceDate, s.party.name, s.party.gstin, "
                    + "s.party.stateCode, CAST(s.party.type AS string), "
                    + "s.totalTaxableAmount, s.totalCgstAmount, s.totalSgstAmount, s.totalIgstAmount, s.grandTotal) "
                    + "FROM SalesInvoice s")
    List<GstRegisterEntryDto> findGstr1Data();

    @Query(
            "SELECT SUM(s.totalTaxableAmount), SUM(s.totalIgstAmount), SUM(s.totalCgstAmount), SUM(s.totalSgstAmount) "
                    + "FROM SalesInvoice s")
    List<Object[]> findTotalTaxValues();

    @Query("SELECT SUM(s.grandTotal) FROM SalesInvoice s WHERE s.invoiceDate = CURRENT_DATE")
    java.math.BigDecimal findTotalSalesToday();
}
