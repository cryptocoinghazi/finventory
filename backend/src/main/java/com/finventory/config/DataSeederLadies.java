package com.finventory.config;

import com.finventory.dto.PurchaseInvoiceDto;
import com.finventory.dto.PurchaseInvoiceLineDto;
import com.finventory.dto.SalesInvoiceDto;
import com.finventory.dto.SalesInvoiceLineDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.Warehouse;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.WarehouseRepository;
import com.finventory.service.PurchaseInvoiceService;
import com.finventory.service.SalesInvoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile({"dev", "local"})
@ConditionalOnProperty(name = "finventory.seed-data", havingValue = "true")
public class DataSeederLadies implements CommandLineRunner {

    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseInvoiceService purchaseInvoiceService;
    private final SalesInvoiceService salesInvoiceService;

    private static final int SEVEN_DAYS = 7;
    private static final int SIX_DAYS = 6;
    private static final int THREE_DAYS = 3;
    private static final int TWO_DAYS = 2;
    private static final int ONE_DAY = 1;

    @Override
    public void run(String... args) {
        Warehouse mainWh = ensureLadiesWarehouse();
        Party vendorA =
                ensureVendor(
                        "Modest Wear Hub",
                        "36MODEST0000H1Z5",
                        "+91-90100-10101",
                        "orders@modestwear.local",
                        "Charminar, Hyderabad");
        Party vendorB =
                ensureVendor(
                        "Veil Couture",
                        "36VEILC0000C1Z5",
                        "+91-90200-20202",
                        "sales@veilcouture.local",
                        "Lad Bazaar, Hyderabad");

        Party walkIn = ensureWalkInCustomer();
        Party ayesha =
                ensureCustomerWithGstin(
                        "Ayesha Fashion",
                        "36AYESHA0000F1Z5",
                        "+91-90300-30303",
                        "accounts@ayeshaladies.local");
        Party zainab =
                ensureCustomerWithGstin(
                        "Zainab Boutique", "36ZAINAB0000C1Z5", "+91-90400-40404", null);
        Party fatimaIndividual = ensureIndividualCustomer("Fatima Begum", "+91-90500-50505");

        List<Item> itemsCreated = new ArrayList<>();
        LadiesItems items = createLadiesItems(itemsCreated);

        if (!itemsCreated.isEmpty() && vendorA != null) {
            seedPurchaseVendorA(mainWh, vendorA, items);
        }
        if (vendorB != null) {
            seedPurchaseVendorB(mainWh, vendorB, items);
        }

        seedSalesWalkIn(mainWh, walkIn, items);
        if (ayesha != null) {
            seedSalesAyesha(mainWh, ayesha, items);
        }
        seedSalesZainabOrFatima(mainWh, zainab, fatimaIndividual, items);
    }

    private Item ensureItem(
            String code,
            String name,
            String hsn,
            BigDecimal taxRate,
            BigDecimal unitPrice,
            String uom,
            List<Item> created) {
        if (itemRepository.existsByCode(code)) {
            return itemRepository.findAll().stream()
                    .filter(i -> code.equals(i.getCode()))
                    .findFirst()
                    .orElse(null);
        }
        Item it =
                itemRepository.save(
                        Item.builder()
                                .name(name)
                                .code(code)
                                .hsnCode(hsn)
                                .taxRate(taxRate)
                                .unitPrice(unitPrice)
                                .uom(uom)
                                .build());
        created.add(it);
        return it;
    }

    private Warehouse ensureLadiesWarehouse() {
        return warehouseRepository.existsByName("Zeemart Ladies Main")
                ? warehouseRepository.findAll().stream()
                        .filter(w -> "Zeemart Ladies Main".equals(w.getName()))
                        .findFirst()
                        .orElseGet(
                                () ->
                                        warehouseRepository.save(
                                                Warehouse.builder()
                                                        .name("Zeemart Ladies Main")
                                                        .stateCode("36")
                                                        .location("Hyderabad")
                                                        .code("ZLM-01")
                                                        .build()))
                : warehouseRepository.save(
                        Warehouse.builder()
                                .name("Zeemart Ladies Main")
                                .stateCode("36")
                                .location("Hyderabad")
                                .code("ZLM-01")
                                .build());
    }

    private Party ensureVendor(
            String name, String gstin, String phone, String email, String address) {
        return partyRepository.existsByGstin(gstin)
                ? partyRepository.findAll().stream()
                        .filter(p -> gstin.equals(p.getGstin()))
                        .findFirst()
                        .orElse(null)
                : partyRepository.save(
                        Party.builder()
                                .name(name)
                                .type(Party.PartyType.VENDOR)
                                .gstin(gstin)
                                .stateCode("36")
                                .phone(phone)
                                .email(email)
                                .address(address)
                                .build());
    }

    private Party ensureWalkInCustomer() {
        return partyRepository.findAll().stream()
                .filter(
                        p ->
                                p.getType() == Party.PartyType.CUSTOMER
                                        && "36".equals(p.getStateCode()))
                .findFirst()
                .orElseGet(
                        () ->
                                partyRepository.save(
                                        Party.builder()
                                                .name("ZLC Walk-in Customer")
                                                .type(Party.PartyType.CUSTOMER)
                                                .stateCode("36")
                                                .build()));
    }

    private Party ensureCustomerWithGstin(String name, String gstin, String phone, String email) {
        return partyRepository.existsByGstin(gstin)
                ? partyRepository.findAll().stream()
                        .filter(p -> gstin.equals(p.getGstin()))
                        .findFirst()
                        .orElse(null)
                : partyRepository.save(
                        Party.builder()
                                .name(name)
                                .type(Party.PartyType.CUSTOMER)
                                .gstin(gstin)
                                .stateCode("36")
                                .phone(phone)
                                .email(email)
                                .build());
    }

    private Party ensureIndividualCustomer(String name, String phone) {
        return partyRepository.findAll().stream()
                .filter(p -> p.getType() == Party.PartyType.CUSTOMER && name.equals(p.getName()))
                .findFirst()
                .orElseGet(
                        () ->
                                partyRepository.save(
                                        Party.builder()
                                                .name(name)
                                                .type(Party.PartyType.CUSTOMER)
                                                .stateCode("36")
                                                .phone(phone)
                                                .build()));
    }

    private LadiesItems createLadiesItems(List<Item> itemsCreated) {
        Item abayaPremium =
                ensureItem(
                        "ABAYA-PREM",
                        "Ladies Abaya Premium (Nida)",
                        "6208",
                        new BigDecimal("12"),
                        new BigDecimal("2499"),
                        "PCS",
                        itemsCreated);
        Item abayaNidaSimple =
                ensureItem(
                        "ABAYA-NIDA-SMP",
                        "Ladies Abaya Nida Simple",
                        "6208",
                        new BigDecimal("12"),
                        new BigDecimal("1799"),
                        "PCS",
                        itemsCreated);
        Item hijabChiffon =
                ensureItem(
                        "HIJAB-CHIFF",
                        "Hijab Chiffon",
                        "6214",
                        new BigDecimal("5"),
                        new BigDecimal("349"),
                        "PCS",
                        itemsCreated);
        Item hijabJersey =
                ensureItem(
                        "HIJAB-JERSEY",
                        "Hijab Jersey",
                        "6214",
                        new BigDecimal("5"),
                        new BigDecimal("399"),
                        "PCS",
                        itemsCreated);
        Item niqabOneEye =
                ensureItem(
                        "NIQAB-1EYE",
                        "Niqab One Eye",
                        "6214",
                        new BigDecimal("5"),
                        new BigDecimal("399"),
                        "PCS",
                        itemsCreated);
        Item niqabThreeEye =
                ensureItem(
                        "NIQAB-3EYE",
                        "Niqab Three Eye",
                        "6214",
                        new BigDecimal("5"),
                        new BigDecimal("449"),
                        "PCS",
                        itemsCreated);
        Item stolePrinted =
                ensureItem(
                        "STOLE-PRNT",
                        "Stole Printed",
                        "6214",
                        new BigDecimal("5"),
                        new BigDecimal("299"),
                        "PCS",
                        itemsCreated);
        Item stolePlain =
                ensureItem(
                        "STOLE-PLN",
                        "Stole Plain",
                        "6214",
                        new BigDecimal("5"),
                        new BigDecimal("249"),
                        "PCS",
                        itemsCreated);
        Item dmNida =
                ensureItem(
                        "DM-NIDA-2_5",
                        "Dress Material Nida 2.5m",
                        "5007",
                        new BigDecimal("12"),
                        new BigDecimal("1499"),
                        "PCS",
                        itemsCreated);
        Item dmCrepe =
                ensureItem(
                        "DM-CREPE-2_5",
                        "Dress Material Crepe 2.5m",
                        "5407",
                        new BigDecimal("12"),
                        new BigDecimal("999"),
                        "PCS",
                        itemsCreated);
        Item dmLinen =
                ensureItem(
                        "DM-LINEN-2_5",
                        "Dress Material Linen 2.5m",
                        "5309",
                        new BigDecimal("12"),
                        new BigDecimal("1199"),
                        "PCS",
                        itemsCreated);
        LadiesItems items = new LadiesItems();
        items.setAbayaPremium(abayaPremium);
        items.setAbayaNidaSimple(abayaNidaSimple);
        items.setHijabChiffon(hijabChiffon);
        items.setHijabJersey(hijabJersey);
        items.setNiqabOneEye(niqabOneEye);
        items.setNiqabThreeEye(niqabThreeEye);
        items.setStolePrinted(stolePrinted);
        items.setStolePlain(stolePlain);
        items.setDmNida(dmNida);
        items.setDmCrepe(dmCrepe);
        items.setDmLinen(dmLinen);
        return items;
    }

    private void seedPurchaseVendorA(Warehouse mainWh, Party vendorA, LadiesItems items) {
        PurchaseInvoiceDto piLadies1 =
                PurchaseInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(SEVEN_DAYS))
                        .partyId(vendorA.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.abayaPremium.getId())
                                                .quantity(new BigDecimal("25"))
                                                .unitPrice(new BigDecimal("2000"))
                                                .taxRate(items.abayaPremium.getTaxRate())
                                                .build(),
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.hijabChiffon.getId())
                                                .quantity(new BigDecimal("100"))
                                                .unitPrice(new BigDecimal("250"))
                                                .taxRate(items.hijabChiffon.getTaxRate())
                                                .build(),
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.stolePrinted.getId())
                                                .quantity(new BigDecimal("120"))
                                                .unitPrice(new BigDecimal("200"))
                                                .taxRate(items.stolePrinted.getTaxRate())
                                                .build()))
                        .build();
        purchaseInvoiceService.createPurchaseInvoice(piLadies1);
    }

    private void seedPurchaseVendorB(Warehouse mainWh, Party vendorB, LadiesItems items) {
        PurchaseInvoiceDto piLadies2 =
                PurchaseInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(SIX_DAYS))
                        .partyId(vendorB.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.niqabThreeEye.getId())
                                                .quantity(new BigDecimal("80"))
                                                .unitPrice(new BigDecimal("350"))
                                                .taxRate(items.niqabThreeEye.getTaxRate())
                                                .build(),
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.dmNida.getId())
                                                .quantity(new BigDecimal("40"))
                                                .unitPrice(new BigDecimal("1300"))
                                                .taxRate(items.dmNida.getTaxRate())
                                                .build(),
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.dmCrepe.getId())
                                                .quantity(new BigDecimal("60"))
                                                .unitPrice(new BigDecimal("800"))
                                                .taxRate(items.dmCrepe.getTaxRate())
                                                .build()))
                        .build();
        purchaseInvoiceService.createPurchaseInvoice(piLadies2);
    }

    private void seedSalesWalkIn(Warehouse mainWh, Party walkIn, LadiesItems items) {
        SalesInvoiceDto siLadies1 =
                SalesInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(THREE_DAYS))
                        .partyId(walkIn.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.hijabJersey.getId())
                                                .quantity(new BigDecimal("6"))
                                                .unitPrice(items.hijabJersey.getUnitPrice())
                                                .taxRate(items.hijabJersey.getTaxRate())
                                                .build(),
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.stolePlain.getId())
                                                .quantity(new BigDecimal("10"))
                                                .unitPrice(items.stolePlain.getUnitPrice())
                                                .taxRate(items.stolePlain.getTaxRate())
                                                .build()))
                        .build();
        salesInvoiceService.createSalesInvoice(siLadies1);
    }

    private void seedSalesAyesha(Warehouse mainWh, Party ayesha, LadiesItems items) {
        SalesInvoiceDto siLadies2 =
                SalesInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(TWO_DAYS))
                        .partyId(ayesha.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.abayaNidaSimple.getId())
                                                .quantity(new BigDecimal("4"))
                                                .unitPrice(items.abayaNidaSimple.getUnitPrice())
                                                .taxRate(items.abayaNidaSimple.getTaxRate())
                                                .build(),
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.dmLinen.getId())
                                                .quantity(new BigDecimal("5"))
                                                .unitPrice(items.dmLinen.getUnitPrice())
                                                .taxRate(items.dmLinen.getTaxRate())
                                                .build()))
                        .build();
        salesInvoiceService.createSalesInvoice(siLadies2);
    }

    private void seedSalesZainabOrFatima(
            Warehouse mainWh, Party zainab, Party fatimaIndividual, LadiesItems items) {
        SalesInvoiceDto siLadies3 =
                SalesInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(ONE_DAY))
                        .partyId(zainab != null ? zainab.getId() : fatimaIndividual.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.abayaPremium.getId())
                                                .quantity(new BigDecimal("3"))
                                                .unitPrice(items.abayaPremium.getUnitPrice())
                                                .taxRate(items.abayaPremium.getTaxRate())
                                                .build(),
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.hijabChiffon.getId())
                                                .quantity(new BigDecimal("8"))
                                                .unitPrice(items.hijabChiffon.getUnitPrice())
                                                .taxRate(items.hijabChiffon.getTaxRate())
                                                .build()))
                        .build();
        salesInvoiceService.createSalesInvoice(siLadies3);
    }

    private static class LadiesItems {
        private Item abayaPremium;
        private Item abayaNidaSimple;
        private Item hijabChiffon;
        private Item hijabJersey;
        private Item niqabOneEye;
        private Item niqabThreeEye;
        private Item stolePrinted;
        private Item stolePlain;
        private Item dmNida;
        private Item dmCrepe;
        private Item dmLinen;

        public void setAbayaPremium(Item abayaPremium) {
            this.abayaPremium = abayaPremium;
        }

        public void setAbayaNidaSimple(Item abayaNidaSimple) {
            this.abayaNidaSimple = abayaNidaSimple;
        }

        public void setHijabChiffon(Item hijabChiffon) {
            this.hijabChiffon = hijabChiffon;
        }

        public void setHijabJersey(Item hijabJersey) {
            this.hijabJersey = hijabJersey;
        }

        public void setNiqabOneEye(Item niqabOneEye) {
            this.niqabOneEye = niqabOneEye;
        }

        public void setNiqabThreeEye(Item niqabThreeEye) {
            this.niqabThreeEye = niqabThreeEye;
        }

        public void setStolePrinted(Item stolePrinted) {
            this.stolePrinted = stolePrinted;
        }

        public void setStolePlain(Item stolePlain) {
            this.stolePlain = stolePlain;
        }

        public void setDmNida(Item dmNida) {
            this.dmNida = dmNida;
        }

        public void setDmCrepe(Item dmCrepe) {
            this.dmCrepe = dmCrepe;
        }

        public void setDmLinen(Item dmLinen) {
            this.dmLinen = dmLinen;
        }
    }
}
