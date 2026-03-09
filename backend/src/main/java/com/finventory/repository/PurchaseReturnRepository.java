package com.finventory.repository;

import com.finventory.model.PurchaseReturn;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, UUID> {
}
