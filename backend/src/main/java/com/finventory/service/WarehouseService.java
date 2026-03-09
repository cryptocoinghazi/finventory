package com.finventory.service;

import com.finventory.dto.WarehouseDto;
import com.finventory.model.Warehouse;
import com.finventory.repository.WarehouseRepository;
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

        Warehouse warehouse =
                Warehouse.builder()
                        .name(dto.getName())
                        .stateCode(dto.getStateCode())
                        .location(dto.getLocation())
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
