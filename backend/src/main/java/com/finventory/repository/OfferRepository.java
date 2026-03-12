package com.finventory.repository;

import com.finventory.model.Offer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID> {

    Optional<Offer> findByCodeIgnoreCase(String code);

    @Query(
            "SELECT o FROM Offer o "
                    + "WHERE o.active = true "
                    + "AND (:asOf IS NULL OR (o.startDate IS NULL OR o.startDate <= :asOf)) "
                    + "AND (:asOf IS NULL OR (o.endDate IS NULL OR o.endDate >= :asOf)) "
                    + "ORDER BY o.updatedAt DESC")
    List<Offer> findActiveAsOf(@Param("asOf") LocalDate asOf);
}
