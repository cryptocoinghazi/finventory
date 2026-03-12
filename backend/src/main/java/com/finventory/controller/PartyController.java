package com.finventory.controller;

import com.finventory.dto.PartyDto;
import com.finventory.model.Party;
import com.finventory.service.PartyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @PostMapping
    public ResponseEntity<PartyDto> createParty(@Valid @RequestBody PartyDto partyDto) {
        return ResponseEntity.ok(partyService.createParty(partyDto));
    }

    @GetMapping
    public ResponseEntity<List<PartyDto>> getAllParties(
            @RequestParam(required = false) Party.PartyType type) {
        return ResponseEntity.ok(partyService.getAllParties(type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyDto> getPartyById(@PathVariable UUID id) {
        return ResponseEntity.ok(partyService.getPartyById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartyDto> updateParty(
            @PathVariable UUID id, @Valid @RequestBody PartyDto partyDto) {
        return ResponseEntity.ok(partyService.updateParty(id, partyDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParty(
            @PathVariable UUID id, @RequestParam(name = "force", defaultValue = "false") boolean force) {
        partyService.deleteParty(id, force);
        return ResponseEntity.noContent().build();
    }
}
