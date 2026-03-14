package com.finventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organization_profile")
public class OrganizationProfile {

    @Id private Long id; // Always 1

    @Column(nullable = false)
    private String companyName;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;

    private String email;
    private String phone;
    private String gstin;
    private String website;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;
}
