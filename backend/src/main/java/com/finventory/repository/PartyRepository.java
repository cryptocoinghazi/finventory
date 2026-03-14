package com.finventory.repository;

import com.finventory.model.Party;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartyRepository extends JpaRepository<Party, UUID> {
    boolean existsByGstin(String gstin);

    Optional<Party> findByGstin(String gstin);

    List<Party> findByNameIgnoreCaseAndType(String name, Party.PartyType type);

    List<Party> findByType(Party.PartyType type);
}
