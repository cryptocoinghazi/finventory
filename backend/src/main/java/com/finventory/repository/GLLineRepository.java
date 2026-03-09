package com.finventory.repository;

import com.finventory.model.GLLine;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GLLineRepository extends JpaRepository<GLLine, UUID> {}
