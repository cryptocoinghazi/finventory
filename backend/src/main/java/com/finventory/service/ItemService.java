package com.finventory.service;

import com.finventory.dto.ItemDto;
import com.finventory.model.Item;
import com.finventory.repository.ItemRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemService {

  private final ItemRepository itemRepository;

  public ItemDto createItem(ItemDto dto) {
    if (itemRepository.existsByCode(dto.getCode())) {
      throw new IllegalArgumentException("Item with code " + dto.getCode() + " already exists");
    }

    Item item =
        Item.builder()
            .name(dto.getName())
            .code(dto.getCode())
            .hsnCode(dto.getHsnCode())
            .taxRate(dto.getTaxRate())
            .unitPrice(dto.getUnitPrice())
            .uom(dto.getUom())
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
            .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + id));

    existing.setName(dto.getName());
    existing.setHsnCode(dto.getHsnCode());
    existing.setTaxRate(dto.getTaxRate());
    existing.setUnitPrice(dto.getUnitPrice());
    existing.setUom(dto.getUom());

    if (!existing.getCode().equals(dto.getCode())) {
      if (itemRepository.existsByCode(dto.getCode())) {
        throw new IllegalArgumentException("Item with code " + dto.getCode() + " already exists");
      }
      existing.setCode(dto.getCode());
    }

    return mapToDto(itemRepository.save(existing));
  }

  public void deleteItem(UUID id) {
    itemRepository.deleteById(id);
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
        .build();
  }
}
