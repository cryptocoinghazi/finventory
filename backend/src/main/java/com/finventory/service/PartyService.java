package com.finventory.service;

import com.finventory.dto.PartyDto;
import com.finventory.model.Party;
import com.finventory.repository.PartyRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartyService {

    private final PartyRepository partyRepository;

    public PartyDto createParty(PartyDto dto) {
        if (dto.getGstin() != null && partyRepository.existsByGstin(dto.getGstin())) {
            throw new IllegalArgumentException(
                    "Party with GSTIN " + dto.getGstin() + " already exists");
        }

        Party party =
                Party.builder()
                        .name(dto.getName())
                        .type(dto.getType())
                        .gstin(dto.getGstin())
                        .stateCode(dto.getStateCode())
                        .address(dto.getAddress())
                        .phone(dto.getPhone())
                        .email(dto.getEmail())
                        .build();

        Party saved = partyRepository.save(party);
        return mapToDto(saved);
    }

    public List<PartyDto> getAllParties(Party.PartyType type) {
        List<Party> parties;
        if (type != null) {
            parties = partyRepository.findByType(type);
        } else {
            parties = partyRepository.findAll();
        }
        return parties.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public PartyDto getPartyById(UUID id) {
        return partyRepository
                .findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Party not found with id: " + id));
    }

    public PartyDto updateParty(UUID id, PartyDto dto) {
        Party existing =
                partyRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Party not found with id: " + id));

        existing.setName(dto.getName());
        existing.setType(dto.getType());
        existing.setStateCode(dto.getStateCode());
        existing.setAddress(dto.getAddress());
        existing.setPhone(dto.getPhone());
        existing.setEmail(dto.getEmail());
        // GSTIN update logic can be complex due to uniqueness, simplified here
        if (dto.getGstin() != null && !dto.getGstin().equals(existing.getGstin())) {
            if (partyRepository.existsByGstin(dto.getGstin())) {
                throw new IllegalArgumentException(
                        "Party with GSTIN " + dto.getGstin() + " already exists");
            }
            existing.setGstin(dto.getGstin());
        }

        return mapToDto(partyRepository.save(existing));
    }

    public void deleteParty(UUID id) {
        partyRepository.deleteById(id);
    }

    private PartyDto mapToDto(Party party) {
        return PartyDto.builder()
                .id(party.getId())
                .name(party.getName())
                .type(party.getType())
                .gstin(party.getGstin())
                .stateCode(party.getStateCode())
                .address(party.getAddress())
                .phone(party.getPhone())
                .email(party.getEmail())
                .build();
    }
}
