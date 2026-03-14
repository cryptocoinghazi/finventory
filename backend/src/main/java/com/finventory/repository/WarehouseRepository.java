package com.finventory.repository;

import com.finventory.model.Warehouse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    boolean existsByName(String name);

    Optional<Warehouse> findByName(String name);

    boolean existsByCode(String code);

    Optional<Warehouse> findByCode(String code);
}
