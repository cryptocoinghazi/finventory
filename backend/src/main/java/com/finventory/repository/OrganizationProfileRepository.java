package com.finventory.repository;

import com.finventory.model.OrganizationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationProfileRepository extends JpaRepository<OrganizationProfile, Long> {
}
