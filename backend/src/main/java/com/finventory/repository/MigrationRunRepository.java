package com.finventory.repository;

import com.finventory.model.MigrationRun;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationRunRepository extends JpaRepository<MigrationRun, UUID> {}
