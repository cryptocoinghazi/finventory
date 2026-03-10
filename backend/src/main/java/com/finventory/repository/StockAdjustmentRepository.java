package com.finventory.repository;

import com.finventory.model.StockAdjustment;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {
    List<StockAdjustment> findAllByOrderByAdjustmentDateDesc(Pageable pageable);

    long countByAdjustmentDate(LocalDate adjustmentDate);
}
