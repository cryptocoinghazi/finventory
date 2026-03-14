package com.finventory.repository;

import com.finventory.dto.GstRegisterEntryDto;
import com.finventory.model.InvoicePaymentStatus;
import com.finventory.model.SalesInvoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, UUID> {
    Optional<SalesInvoice> findByInvoiceNumber(String invoiceNumber);

    List<SalesInvoice> findAllByOrderByInvoiceDateDesc(Pageable pageable);

    long countByInvoiceDate(LocalDate invoiceDate);

    void deleteByPartyId(UUID partyId);

    List<SalesInvoice> findAllByPartyId(UUID partyId);

    @Query(
            "SELECT s FROM SalesInvoice s "
                    + "WHERE s.paymentStatus = COALESCE(:paymentStatus, s.paymentStatus) "
                    + "AND s.invoiceDate >= COALESCE(:fromDate, s.invoiceDate) "
                    + "AND s.invoiceDate <= COALESCE(:toDate, s.invoiceDate) "
                    + "ORDER BY s.invoiceDate DESC")
    List<SalesInvoice> findAllWithFilters(
            @Param("paymentStatus") InvoicePaymentStatus paymentStatus,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

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

    @Query(
            "SELECT DISTINCT s FROM SalesInvoice s "
                    + "JOIN FETCH s.party p "
                    + "JOIN FETCH s.warehouse w "
                    + "LEFT JOIN FETCH s.lines l "
                    + "LEFT JOIN FETCH l.item i "
                    + "WHERE s.invoiceDate >= COALESCE(:fromDate, s.invoiceDate) "
                    + "AND s.invoiceDate <= COALESCE(:toDate, s.invoiceDate) "
                    + "AND p.id = COALESCE(:partyId, p.id) "
                    + "AND s.paymentStatus = COALESCE(:paymentStatus, s.paymentStatus) "
                    + "AND i.id = COALESCE(:itemId, i.id) "
                    + "AND (COALESCE(:category, '') = '' OR i.category = :category) "
                    + "ORDER BY s.invoiceDate ASC")
    List<SalesInvoice> findForSalesReport(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("partyId") UUID partyId,
            @Param("paymentStatus") InvoicePaymentStatus paymentStatus,
            @Param("itemId") UUID itemId,
            @Param("category") String category);
}
