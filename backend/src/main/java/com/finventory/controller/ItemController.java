package com.finventory.controller;

import com.finventory.dto.ItemDto;
import com.finventory.service.ItemService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

  private final ItemService itemService;

  @PostMapping
  public ResponseEntity<ItemDto> createItem(@Valid @RequestBody ItemDto itemDto) {
    return ResponseEntity.ok(itemService.createItem(itemDto));
  }

  @GetMapping
  public ResponseEntity<List<ItemDto>> getAllItems() {
    return ResponseEntity.ok(itemService.getAllItems());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ItemDto> getItemById(@PathVariable UUID id) {
    return ResponseEntity.ok(itemService.getItemById(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ItemDto> updateItem(@PathVariable UUID id, @Valid @RequestBody ItemDto itemDto) {
    return ResponseEntity.ok(itemService.updateItem(id, itemDto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteItem(@PathVariable UUID id) {
    itemService.deleteItem(id);
    return ResponseEntity.noContent().build();
  }
}
