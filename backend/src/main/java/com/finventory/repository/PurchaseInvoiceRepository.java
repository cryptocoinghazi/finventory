package com.finventory.repository;

import com.finventory.model.PurchaseInvoice;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, UUID> {
  boolean existsByInvoiceNumber(String invoiceNumber);
}
