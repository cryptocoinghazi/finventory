package com.finventory.repository;

import com.finventory.model.SalesReturn;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, UUID> {
}
