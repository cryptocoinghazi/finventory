package com.finventory.controller;

import com.finventory.dto.OrganizationProfileDto;
import com.finventory.service.OrganizationProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/organization")
@RequiredArgsConstructor
public class OrganizationProfileController {

    private final OrganizationProfileService service;

    @GetMapping
    public ResponseEntity<OrganizationProfileDto> getProfile() {
        return ResponseEntity.ok(service.getOrganizationProfile());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<OrganizationProfileDto> updateProfile(
            @RequestBody OrganizationProfileDto dto) {
        return ResponseEntity.ok(service.updateOrganizationProfile(dto));
    }
}
