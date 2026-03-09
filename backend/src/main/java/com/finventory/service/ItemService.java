package com.finventory.service;

import com.finventory.dto.ItemCsvDto;
import com.finventory.dto.ItemDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;

    @Transactional
    public List<ItemDto> uploadItems(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<ItemCsvDto> csvToBean =
                    new CsvToBeanBuilder<ItemCsvDto>(reader)
                            .withType(ItemCsvDto.class)
                            .withIgnoreLeadingWhiteSpace(true)
                            .build();

            List<ItemCsvDto> csvDtos = csvToBean.parse();
            List<Item> itemsToSave = new ArrayList<>();

            // Find a default vendor to assign to items
            List<Party> vendors = partyRepository.findByType(Party.PartyType.VENDOR);
            Party defaultVendor;
            if (!vendors.isEmpty()) {
                defaultVendor = vendors.get(0);
            } else {
                defaultVendor = Party.builder()
                        .name("General Vendor")
                        .type(Party.PartyType.VENDOR)
                        .address("Default Address")
                        .build();
                partyRepository.save(defaultVendor);
            }

            for (ItemCsvDto csvDto : csvDtos) {
                Optional<Item> existing = itemRepository.findByCode(csvDto.getCode());
                if (existing.isPresent()) {
                    Item item = existing.get();
                    item.setName(csvDto.getName());
                    item.setHsnCode(csvDto.getHsnCode());
                    item.setTaxRate(csvDto.getTaxRate());
                    item.setUnitPrice(csvDto.getUnitPrice());
                    item.setUom(csvDto.getUom());
                    if (item.getVendor() == null) {
                        item.setVendor(defaultVendor);
                    }
                    itemsToSave.add(item);
                } else {
                    Item item =
                            Item.builder()
                                    .vendor(defaultVendor)
                                    .name(csvDto.getName())
                                    .code(csvDto.getCode())
                                    .hsnCode(csvDto.getHsnCode())
                                    .taxRate(csvDto.getTaxRate())
                                    .unitPrice(csvDto.getUnitPrice())
                                    .uom(csvDto.getUom())
                                    .build();
                    itemsToSave.add(item);
                }
            }

            List<Item> savedItems = itemRepository.saveAll(itemsToSave);
            return savedItems.stream().map(this::mapToDto).collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    public ItemDto createItem(ItemDto dto) {
        if (itemRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException(
                    "Item with code " + dto.getCode() + " already exists");
        }

        Party vendor;
        if (dto.getVendorId() != null) {
            vendor = partyRepository.findById(dto.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
            if (vendor.getType() != Party.PartyType.VENDOR) {
                throw new IllegalArgumentException("Selected party is not a vendor");
            }
        } else {
            vendor = getOrCreateDefaultVendor();
        }

        Item item =
                Item.builder()
                        .name(dto.getName())
                        .code(dto.getCode())
                        .hsnCode(dto.getHsnCode())
                        .taxRate(dto.getTaxRate())
                        .unitPrice(dto.getUnitPrice())
                        .uom(dto.getUom())
                        .vendor(vendor)
                        .build();

        Item saved = itemRepository.save(item);
        return mapToDto(saved);
    }

    public List<ItemDto> getAllItems() {
        return itemRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public ItemDto getItemById(UUID id) {
        return itemRepository
                .findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));
    }

    public ItemDto updateItem(UUID id, ItemDto dto) {
        Item existing =
                itemRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Item not found with id: " + id));

        existing.setName(dto.getName());
        existing.setHsnCode(dto.getHsnCode());
        existing.setTaxRate(dto.getTaxRate());
        existing.setUnitPrice(dto.getUnitPrice());
        existing.setUom(dto.getUom());

        if (dto.getVendorId() != null) {
            Party vendor = partyRepository.findById(dto.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
            if (vendor.getType() != Party.PartyType.VENDOR) {
                throw new IllegalArgumentException("Selected party is not a vendor");
            }
            existing.setVendor(vendor);
        } else {
            existing.setVendor(getOrCreateDefaultVendor());
        }

        if (!existing.getCode().equals(dto.getCode())) {
            if (itemRepository.existsByCode(dto.getCode())) {
                throw new IllegalArgumentException(
                        "Item with code " + dto.getCode() + " already exists");
            }
            existing.setCode(dto.getCode());
        }

        return mapToDto(itemRepository.save(existing));
    }

    public void deleteItem(UUID id) {
        itemRepository.deleteById(id);
    }

    private Party getOrCreateDefaultVendor() {
        List<Party> vendors = partyRepository.findByType(Party.PartyType.VENDOR);
        if (!vendors.isEmpty()) {
            return vendors.get(0);
        }
        Party defaultVendor = Party.builder()
                .name("General Vendor")
                .type(Party.PartyType.VENDOR)
                .address("Default Address")
                .build();
        return partyRepository.save(defaultVendor);
    }

    private ItemDto mapToDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .code(item.getCode())
                .hsnCode(item.getHsnCode())
                .taxRate(item.getTaxRate())
                .unitPrice(item.getUnitPrice())
                .uom(item.getUom())
                .vendorId(item.getVendor() != null ? item.getVendor().getId() : null)
                .vendorName(item.getVendor() != null ? item.getVendor().getName() : null)
                .build();
    }
}
