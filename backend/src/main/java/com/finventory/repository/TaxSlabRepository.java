package com.finventory.repository;

import com.finventory.model.TaxSlab;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxSlabRepository extends JpaRepository<TaxSlab, UUID> {
    boolean existsByRate(BigDecimal rate);

    Optional<TaxSlab> findByRate(BigDecimal rate);

    List<TaxSlab> findByDescriptionContainingIgnoreCase(String description);
}
