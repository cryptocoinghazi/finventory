package com.finventory.controller;

import com.finventory.dto.TaxSlabDto;
import com.finventory.service.TaxSlabService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tax-slabs")
@RequiredArgsConstructor
public class TaxSlabController {

  private final TaxSlabService taxSlabService;

  @PostMapping
  public ResponseEntity<TaxSlabDto> createTaxSlab(@RequestBody TaxSlabDto taxSlabDto) {
    return ResponseEntity.ok(taxSlabService.createTaxSlab(taxSlabDto));
  }

  @GetMapping
  public ResponseEntity<List<TaxSlabDto>> getAllTaxSlabs() {
    return ResponseEntity.ok(taxSlabService.getAllTaxSlabs());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTaxSlab(@PathVariable UUID id) {
    taxSlabService.deleteTaxSlab(id);
    return ResponseEntity.noContent().build();
  }
}
