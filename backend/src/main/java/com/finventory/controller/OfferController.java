package com.finventory.controller;

import com.finventory.dto.OfferDto;
import com.finventory.dto.OfferValidationRequest;
import com.finventory.dto.OfferValidationResponse;
import com.finventory.service.OfferService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    public ResponseEntity<List<OfferDto>> getOffers(
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "asOfDate", required = false) LocalDate asOfDate) {
        if (active != null && active) {
            return ResponseEntity.ok(offerService.getActiveOffers(asOfDate));
        }
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferDto> getOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(offerService.getOffer(id));
    }

    @PostMapping
    public ResponseEntity<OfferDto> createOffer(@Valid @RequestBody OfferDto dto) {
        OfferDto created = offerService.createOffer(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfferDto> updateOffer(
            @PathVariable UUID id, @Valid @RequestBody OfferDto dto) {
        return ResponseEntity.ok(offerService.updateOffer(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable UUID id) {
        offerService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validate")
    public ResponseEntity<OfferValidationResponse> validateOffer(
            @Valid @RequestBody OfferValidationRequest request) {
        return ResponseEntity.ok(offerService.validateOffer(request));
    }
}
