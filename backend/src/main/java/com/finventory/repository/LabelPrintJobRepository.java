package com.finventory.repository;

import com.finventory.model.LabelPrintJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelPrintJobRepository extends JpaRepository<LabelPrintJob, UUID> {}

