package com.finventory.repository;

import com.finventory.model.MigrationStageExecution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationStageExecutionRepository
        extends JpaRepository<MigrationStageExecution, UUID> {
    Optional<MigrationStageExecution> findByRunIdAndStageKey(UUID runId, String stageKey);

    List<MigrationStageExecution> findByRunIdOrderByStartedAtAsc(UUID runId);
}
