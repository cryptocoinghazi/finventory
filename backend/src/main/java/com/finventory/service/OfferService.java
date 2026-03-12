package com.finventory.service;

import com.finventory.dto.OfferDto;
import com.finventory.dto.OfferValidationRequest;
import com.finventory.dto.OfferValidationResponse;
import com.finventory.model.Item;
import com.finventory.model.Offer;
import com.finventory.model.OfferDiscountType;
import com.finventory.model.OfferScope;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.OfferRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int PERCENT_CALC_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    private final OfferRepository offerRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<OfferDto> getAllOffers() {
        return offerRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<OfferDto> getActiveOffers(LocalDate asOfDate) {
        LocalDate asOf = asOfDate != null ? asOfDate : LocalDate.now();
        return offerRepository.findActiveAsOf(asOf).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OfferDto getOffer(UUID id) {
        Offer offer =
                offerRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Offer not found"));
        return toDto(offer);
    }

    @Transactional
    public OfferDto createOffer(OfferDto dto) {
        normalizeDto(dto);

        if (dto.getCode() != null && offerRepository.findByCodeIgnoreCase(dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Offer code already exists");
        }

        Item item = resolveItem(dto.getScope(), dto.getItemId());

        Offer offer =
                Offer.builder()
                        .name(dto.getName())
                        .code(dto.getCode())
                        .discountType(dto.getDiscountType())
                        .scope(dto.getScope())
                        .discountValue(dto.getDiscountValue())
                        .item(item)
                        .startDate(dto.getStartDate())
                        .endDate(dto.getEndDate())
                        .active(dto.getActive() != null ? dto.getActive() : true)
                        .usageLimit(dto.getUsageLimit())
                        .usedCount(dto.getUsedCount() != null ? dto.getUsedCount() : 0)
                        .minBillAmount(dto.getMinBillAmount())
                        .build();

        Offer saved = offerRepository.save(offer);
        return toDto(saved);
    }

    @Transactional
    public OfferDto updateOffer(UUID id, OfferDto dto) {
        normalizeDto(dto);

        Offer offer =
                offerRepository
                        .findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        if (dto.getCode() != null) {
            offerRepository
                    .findByCodeIgnoreCase(dto.getCode())
                    .filter((o) -> !o.getId().equals(offer.getId()))
                    .ifPresent((o) -> {
                        throw new IllegalArgumentException("Offer code already exists");
                    });
        }

        Item item = resolveItem(dto.getScope(), dto.getItemId());

        offer.setName(dto.getName());
        offer.setCode(dto.getCode());
        offer.setDiscountType(dto.getDiscountType());
        offer.setScope(dto.getScope());
        offer.setDiscountValue(dto.getDiscountValue());
        offer.setItem(item);
        offer.setStartDate(dto.getStartDate());
        offer.setEndDate(dto.getEndDate());
        if (dto.getActive() != null) {
            offer.setActive(dto.getActive());
        }
        offer.setUsageLimit(dto.getUsageLimit());
        if (dto.getUsedCount() != null) {
            offer.setUsedCount(dto.getUsedCount());
        }
        offer.setMinBillAmount(dto.getMinBillAmount());

        Offer saved = offerRepository.save(offer);
        return toDto(saved);
    }

    @Transactional
    public void deleteOffer(UUID id) {
        if (!offerRepository.existsById(id)) {
            throw new EntityNotFoundException("Offer not found");
        }
        offerRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public OfferValidationResponse validateOffer(OfferValidationRequest request) {
        String code = request.getCode() != null ? request.getCode().trim() : null;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code is required");
        }

        Offer offer =
                offerRepository
                        .findByCodeIgnoreCase(code)
                        .orElseThrow(() -> new EntityNotFoundException("Offer not found"));

        if (offer.getActive() == null || !offer.getActive()) {
            throw new IllegalArgumentException("Offer is inactive");
        }

        LocalDate asOf = request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now();
        if (offer.getStartDate() != null && asOf.isBefore(offer.getStartDate())) {
            throw new IllegalArgumentException("Offer is not yet active");
        }
        if (offer.getEndDate() != null && asOf.isAfter(offer.getEndDate())) {
            throw new IllegalArgumentException("Offer has expired");
        }

        if (offer.getUsageLimit() != null) {
            int used = offer.getUsedCount() != null ? offer.getUsedCount() : 0;
            if (used >= offer.getUsageLimit()) {
                throw new IllegalArgumentException("Offer usage limit reached");
            }
        }

        BigDecimal taxableSubtotal = request.getTaxableSubtotal();
        if (taxableSubtotal == null || taxableSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Taxable subtotal must be greater than 0");
        }

        if (offer.getMinBillAmount() != null
                && taxableSubtotal.compareTo(offer.getMinBillAmount()) < 0) {
            throw new IllegalArgumentException("Minimum bill amount not met");
        }

        BigDecimal discountAmount =
                switch (offer.getScope()) {
                    case CART -> computeDiscount(offer.getDiscountType(), offer.getDiscountValue(), taxableSubtotal);
                    case ITEM -> computeItemScopedDiscount(offer, request);
                };

        return OfferValidationResponse.builder()
                .offerId(offer.getId())
                .code(offer.getCode())
                .name(offer.getName())
                .scope(offer.getScope())
                .discountType(offer.getDiscountType())
                .discountValue(offer.getDiscountValue())
                .discountAmount(discountAmount)
                .build();
    }

    private BigDecimal computeItemScopedDiscount(Offer offer, OfferValidationRequest request) {
        if (offer.getItem() == null) {
            throw new IllegalArgumentException("Item-specific offer is missing item");
        }
        UUID itemId = offer.getItem().getId();
        BigDecimal itemSubtotal = BigDecimal.ZERO;
        for (OfferValidationRequest.OfferValidationLine line : request.getLines()) {
            if (line.getItemId() == null || !line.getItemId().equals(itemId)) {
                continue;
            }
            BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = line.getUnitPrice() != null ? line.getUnitPrice() : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.ZERO) < 0) {
                continue;
            }
            itemSubtotal = itemSubtotal.add(qty.multiply(price));
        }
        if (itemSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Offer does not apply to cart items");
        }
        return computeDiscount(offer.getDiscountType(), offer.getDiscountValue(), itemSubtotal);
    }

    private static BigDecimal computeDiscount(
            OfferDiscountType discountType, BigDecimal discountValue, BigDecimal baseAmount) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid discount value");
        }
        BigDecimal raw =
                switch (discountType) {
                    case PERCENT ->
                            baseAmount
                                    .multiply(discountValue)
                                    .divide(HUNDRED, PERCENT_CALC_SCALE, RoundingMode.HALF_UP);
                    case FLAT -> discountValue;
                };
        BigDecimal capped = raw.min(baseAmount);
        return capped.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private Item resolveItem(OfferScope scope, UUID itemId) {
        if (scope == OfferScope.ITEM) {
            if (itemId == null) {
                throw new IllegalArgumentException("Item ID is required for item-specific offer");
            }
            return itemRepository
                    .findById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("Item not found"));
        }
        return null;
    }

    private static void normalizeDto(OfferDto dto) {
        if (dto.getCode() != null) {
            String code = dto.getCode().trim();
            dto.setCode(code.isBlank() ? null : code);
        }
        if (dto.getName() != null) {
            dto.setName(dto.getName().trim());
        }
    }

    private OfferDto toDto(Offer offer) {
        return OfferDto.builder()
                .id(offer.getId())
                .name(offer.getName())
                .code(offer.getCode())
                .discountType(offer.getDiscountType())
                .scope(offer.getScope())
                .discountValue(offer.getDiscountValue())
                .itemId(offer.getItem() != null ? offer.getItem().getId() : null)
                .startDate(offer.getStartDate())
                .endDate(offer.getEndDate())
                .active(offer.getActive())
                .usageLimit(offer.getUsageLimit())
                .usedCount(offer.getUsedCount())
                .minBillAmount(offer.getMinBillAmount())
                .build();
    }
}
