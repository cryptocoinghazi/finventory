package com.finventory.repository;

import com.finventory.model.MigrationIdMap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationIdMapRepository extends JpaRepository<MigrationIdMap, Long> {
    Optional<MigrationIdMap> findBySourceSystemAndEntityTypeAndSourceId(
            String sourceSystem, String entityType, Long sourceId);

    Optional<MigrationIdMap> findByEntityTypeAndTargetId(String entityType, UUID targetId);
}
