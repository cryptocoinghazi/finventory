package com.finventory.repository;

import com.finventory.model.SalesInvoice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, UUID> {
  Optional<SalesInvoice> findByInvoiceNumber(String invoiceNumber);
}
