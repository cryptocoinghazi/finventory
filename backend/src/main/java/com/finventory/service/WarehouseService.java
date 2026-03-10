package com.finventory.service;

import com.finventory.dto.WarehouseDto;
import com.finventory.model.Warehouse;
import com.finventory.repository.WarehouseRepository;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseDto createWarehouse(WarehouseDto dto) {
        if (warehouseRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException(
                    "Warehouse with name " + dto.getName() + " already exists");
        }

        String code = generateWarehouseCode(dto.getName());
        Warehouse warehouse =
                Warehouse.builder()
                        .name(dto.getName())
                        .stateCode(dto.getStateCode())
                        .location(dto.getLocation())
                        .code(code)
                        .build();

        return mapToDto(warehouseRepository.save(warehouse));
    }

    public List<WarehouseDto> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public WarehouseDto getWarehouseById(UUID id) {
        return warehouseRepository
                .findById(id)
                .map(this::mapToDto)
                .orElseThrow(
                        () -> new IllegalArgumentException("Warehouse not found with id: " + id));
    }

    public WarehouseDto updateWarehouse(UUID id, WarehouseDto dto) {
        Warehouse warehouse =
                warehouseRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Warehouse not found with id: " + id));

        // Check name uniqueness if changed
        if (!warehouse.getName().equals(dto.getName())
                && warehouseRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException(
                    "Warehouse with name " + dto.getName() + " already exists");
        }

        warehouse.setName(dto.getName());
        warehouse.setStateCode(dto.getStateCode());
        warehouse.setLocation(dto.getLocation());
        if (warehouse.getCode() == null || warehouse.getCode().isBlank()) {
            warehouse.setCode(generateWarehouseCode(dto.getName()));
        }

        return mapToDto(warehouseRepository.save(warehouse));
    }

    private String generateWarehouseCode(String name) {
        String base = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < base.length() && sb.length() < 10; i++) {
            char ch = base.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            }
        }
        if (sb.isEmpty()) {
            sb.append("WH");
        }
        String candidate = sb.toString();
        if (!warehouseRepository.existsByCode(candidate)) {
            return candidate;
        }

        String prefix = candidate.length() > 8 ? candidate.substring(0, 8) : candidate;
        for (int suffix = 1; suffix <= 99; suffix++) {
            String next = prefix + String.format(Locale.ROOT, "%02d", suffix);
            if (!warehouseRepository.existsByCode(next)) {
                return next;
            }
        }

        return prefix + String.format(Locale.ROOT, "%02d", Math.abs(name.hashCode()) % 100);
    }

    public void deleteWarehouse(UUID id) {
        warehouseRepository.deleteById(id);
    }

    private WarehouseDto mapToDto(Warehouse warehouse) {
        return WarehouseDto.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .stateCode(warehouse.getStateCode())
                .location(warehouse.getLocation())
                .build();
    }
}
