package com.finventory.repository;

import com.finventory.model.DocumentSequence;
import com.finventory.model.SequenceType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentSequence> findBySequenceTypeAndWarehouseIdAndFinancialYear(
            SequenceType sequenceType, UUID warehouseId, String financialYear);
}
