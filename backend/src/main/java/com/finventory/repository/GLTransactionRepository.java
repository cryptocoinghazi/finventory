package com.finventory.repository;

import com.finventory.model.GLTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GLTransactionRepository extends JpaRepository<GLTransaction, UUID> {
}
