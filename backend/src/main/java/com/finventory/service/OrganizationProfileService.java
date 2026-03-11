package com.finventory.service;

import com.finventory.dto.OrganizationProfileDto;
import com.finventory.model.OrganizationProfile;
import com.finventory.repository.OrganizationProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationProfileService {

    private final OrganizationProfileRepository repository;

    public OrganizationProfileDto getOrganizationProfile() {
        return repository.findById(1L).map(this::mapToDto).orElse(new OrganizationProfileDto());
    }

    public OrganizationProfileDto updateOrganizationProfile(OrganizationProfileDto dto) {
        var profile = repository.findById(1L).orElse(new OrganizationProfile());
        profile.setId(1L);
        profile.setCompanyName(dto.getCompanyName());
        profile.setAddressLine1(dto.getAddressLine1());
        profile.setAddressLine2(dto.getAddressLine2());
        profile.setCity(dto.getCity());
        profile.setState(dto.getState());
        profile.setPincode(dto.getPincode());
        profile.setEmail(dto.getEmail());
        profile.setPhone(dto.getPhone());
        profile.setGstin(dto.getGstin());
        profile.setWebsite(dto.getWebsite());
        profile.setLogoUrl(dto.getLogoUrl());

        repository.save(profile);
        return mapToDto(profile);
    }

    private OrganizationProfileDto mapToDto(OrganizationProfile entity) {
        return OrganizationProfileDto.builder()
                .companyName(entity.getCompanyName())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .city(entity.getCity())
                .state(entity.getState())
                .pincode(entity.getPincode())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .gstin(entity.getGstin())
                .website(entity.getWebsite())
                .logoUrl(entity.getLogoUrl())
                .build();
    }
}
