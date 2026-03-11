package com.finventory.service;

import com.finventory.dto.TaxSlabDto;
import com.finventory.model.TaxSlab;
import com.finventory.repository.TaxSlabRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxSlabService {

    private final TaxSlabRepository taxSlabRepository;

    public TaxSlabDto createTaxSlab(TaxSlabDto dto) {
        if (taxSlabRepository.existsByRate(dto.getRate())) {
            throw new IllegalArgumentException(
                    "Tax slab with rate " + dto.getRate() + " already exists");
        }

        TaxSlab taxSlab =
                TaxSlab.builder().rate(dto.getRate()).description(dto.getDescription()).build();

        return mapToDto(taxSlabRepository.save(taxSlab));
    }

    public List<TaxSlabDto> getAllTaxSlabs(String search) {
        List<TaxSlab> slabs;
        if (search == null || search.isBlank()) {
            slabs = taxSlabRepository.findAll();
        } else {
            slabs = taxSlabRepository.findByDescriptionContainingIgnoreCase(search);
        }
        return slabs.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public TaxSlabDto getTaxSlabById(UUID id) {
        return taxSlabRepository
                .findById(id)
                .map(this::mapToDto)
                .orElseThrow(
                        () -> new IllegalArgumentException("Tax slab not found with id: " + id));
    }

    public TaxSlabDto updateTaxSlab(UUID id, TaxSlabDto dto) {
        TaxSlab taxSlab =
                taxSlabRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Tax slab not found with id: " + id));

        // Check rate uniqueness if changed
        if (taxSlab.getRate().compareTo(dto.getRate()) != 0
                && taxSlabRepository.existsByRate(dto.getRate())) {
            throw new IllegalArgumentException(
                    "Tax slab with rate " + dto.getRate() + " already exists");
        }

        taxSlab.setRate(dto.getRate());
        taxSlab.setDescription(dto.getDescription());

        return mapToDto(taxSlabRepository.save(taxSlab));
    }

    public void deleteTaxSlab(UUID id) {
        taxSlabRepository.deleteById(id);
    }

    private TaxSlabDto mapToDto(TaxSlab taxSlab) {
        return TaxSlabDto.builder()
                .id(taxSlab.getId())
                .rate(taxSlab.getRate())
                .description(taxSlab.getDescription())
                .build();
    }
}
