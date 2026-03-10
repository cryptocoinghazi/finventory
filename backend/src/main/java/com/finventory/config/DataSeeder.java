package com.finventory.config;

import com.finventory.dto.PurchaseInvoiceDto;
import com.finventory.dto.PurchaseInvoiceLineDto;
import com.finventory.dto.SalesInvoiceDto;
import com.finventory.dto.SalesInvoiceLineDto;
import com.finventory.dto.PurchaseReturnDto;
import com.finventory.dto.PurchaseReturnLineDto;
import com.finventory.dto.SalesReturnDto;
import com.finventory.dto.SalesReturnLineDto;
import com.finventory.dto.StockAdjustmentDto;
import com.finventory.model.Item;
import com.finventory.model.Party;
import com.finventory.model.Warehouse;
import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.repository.ItemRepository;
import com.finventory.repository.PartyRepository;
import com.finventory.repository.UserRepository;
import com.finventory.repository.WarehouseRepository;
import com.finventory.service.PurchaseInvoiceService;
import com.finventory.service.SalesInvoiceService;
import com.finventory.service.PurchaseReturnService;
import com.finventory.service.SalesReturnService;
import com.finventory.service.StockAdjustmentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile({"dev", "local"})
public class DataSeeder implements CommandLineRunner {

    private final ItemRepository itemRepository;
    private final PartyRepository partyRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseInvoiceService purchaseInvoiceService;
    private final SalesInvoiceService salesInvoiceService;
    private final PurchaseReturnService purchaseReturnService;
    private final SalesReturnService salesReturnService;
    private final StockAdjustmentService stockAdjustmentService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int TEN_DAYS = 10;
    private static final int EIGHT_DAYS = 8;
    private static final int FIVE_DAYS = 5;
    private static final int THREE_DAYS = 3;
    private static final int ONE_DAY = 1;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            userRepository.save(
                    User.builder()
                            .username("admin")
                            .email("admin@finventory.local")
                            .password(passwordEncoder.encode("admin"))
                            .role(Role.ADMIN)
                            .build());
        }

        if (itemRepository.count() > 0) {
            return;
        }
        Warehouse mainWh = createMainWarehouse();
        Party vendor1 =
                createVendor(
                        "Royal Fabrics Suppliers",
                        "36ROYAL0000F1Z5",
                        "Begum Bazaar, Hyderabad",
                        "+91-90000-11111",
                        "sales@royalfabrics.local");
        Party vendor2 =
                createVendor(
                        "Abaya World Distributors",
                        "36ABAYA0000D1Z5",
                        "Moghalpura, Hyderabad",
                        "+91-90000-22222",
                        "orders@abayaworld.local");
        Party walkIn = createCustomer("Walk-in Customer", null, "36", null, null);
        Party ahmed = createCustomer("Ahmed Khan", null, "36", "+91-98888-33333", null);
        Party sara =
                createCustomer(
                        "Sara Boutique",
                        "36SARA0000B1Z5",
                        "36",
                        "+91-97777-44444",
                        "accounts@saraboutique.local");

        Items items = createItems();

        List<PurchaseInvoiceDto> purchaseInvoices =
                seedPurchaseInvoices(mainWh, vendor1, vendor2, items);
        List<SalesInvoiceDto> salesInvoices = seedSalesInvoices(mainWh, walkIn, ahmed, sara, items);
        seedReturnsAndAdjustments(mainWh, vendor1, walkIn, items, purchaseInvoices, salesInvoices);
    }

    private Warehouse createMainWarehouse() {
        return warehouseRepository.save(
                Warehouse.builder()
                        .name("Main Warehouse")
                        .stateCode("36")
                        .location("Hyderabad")
                        .code("MAIN")
                        .build());
    }

    private Party createVendor(
            String name, String gstin, String address, String phone, String email) {
        return partyRepository.save(
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

    private Party createCustomer(
            String name, String gstin, String stateCode, String phone, String email) {
        Party.PartyType type = Party.PartyType.CUSTOMER;
        return partyRepository.save(
                Party.builder()
                        .name(name)
                        .type(type)
                        .gstin(gstin)
                        .stateCode(stateCode)
                        .phone(phone)
                        .email(email)
                        .build());
    }

    private Items createItems() {
        Item kurta =
                itemRepository.save(
                        Item.builder()
                                .name("Men Kurta Cotton")
                                .code("KURTA-M-COT")
                                .hsnCode("6105")
                                .taxRate(new BigDecimal("12"))
                                .unitPrice(new BigDecimal("899"))
                                .uom("PCS")
                                .build());
        Item abaya =
                itemRepository.save(
                        Item.builder()
                                .name("Ladies Abaya Classic")
                                .code("ABAYA-L-CLS")
                                .hsnCode("6208")
                                .taxRate(new BigDecimal("12"))
                                .unitPrice(new BigDecimal("1999"))
                                .uom("PCS")
                                .build());
        Item kidsSet =
                itemRepository.save(
                        Item.builder()
                                .name("Kids Dress Set")
                                .code("KIDS-SET")
                                .hsnCode("6111")
                                .taxRate(new BigDecimal("12"))
                                .unitPrice(new BigDecimal("749"))
                                .uom("PCS")
                                .build());
        Item cottonDM =
                itemRepository.save(
                        Item.builder()
                                .name("Cotton Dress Material 2.5m")
                                .code("DM-COT-2_5")
                                .hsnCode("5208")
                                .taxRate(new BigDecimal("5"))
                                .unitPrice(new BigDecimal("399"))
                                .uom("PCS")
                                .build());
        Item silkDM =
                itemRepository.save(
                        Item.builder()
                                .name("Silk Dress Material 2.5m")
                                .code("DM-SILK-2_5")
                                .hsnCode("5007")
                                .taxRate(new BigDecimal("12"))
                                .unitPrice(new BigDecimal("1299"))
                                .uom("PCS")
                                .build());
        Item hijab =
                itemRepository.save(
                        Item.builder()
                                .name("Plain Hijab")
                                .code("HIJAB-PLN")
                                .hsnCode("6214")
                                .taxRate(new BigDecimal("5"))
                                .unitPrice(new BigDecimal("299"))
                                .uom("PCS")
                                .build());
        Item niqab =
                itemRepository.save(
                        Item.builder()
                                .name("Niqab 2-layer")
                                .code("NIQAB-2LY")
                                .hsnCode("6214")
                                .taxRate(new BigDecimal("5"))
                                .unitPrice(new BigDecimal("349"))
                                .uom("PCS")
                                .build());
        Item stole =
                itemRepository.save(
                        Item.builder()
                                .name("Stole Viscose")
                                .code("STOLE-VIS")
                                .hsnCode("6214")
                                .taxRate(new BigDecimal("5"))
                                .unitPrice(new BigDecimal("249"))
                                .uom("PCS")
                                .build());
        Items items = new Items();
        items.setKurta(kurta);
        items.setAbaya(abaya);
        items.setKidsSet(kidsSet);
        items.setCottonDM(cottonDM);
        items.setSilkDM(silkDM);
        items.setHijab(hijab);
        items.setNiqab(niqab);
        items.setStole(stole);
        return items;
    }

    private List<PurchaseInvoiceDto> seedPurchaseInvoices(
            Warehouse mainWh, Party vendor1, Party vendor2, Items items) {
        java.util.ArrayList<PurchaseInvoiceDto> created = new java.util.ArrayList<>();
        PurchaseInvoiceDto pi1 =
                PurchaseInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(TEN_DAYS))
                        .partyId(vendor1.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.kurta.getId())
                                                .quantity(new BigDecimal("30"))
                                                .unitPrice(new BigDecimal("700"))
                                                .taxRate(items.kurta.getTaxRate())
                                                .build(),
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.cottonDM.getId())
                                                .quantity(new BigDecimal("40"))
                                                .unitPrice(new BigDecimal("300"))
                                                .taxRate(items.cottonDM.getTaxRate())
                                                .build(),
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.hijab.getId())
                                                .quantity(new BigDecimal("50"))
                                                .unitPrice(new BigDecimal("200"))
                                                .taxRate(items.hijab.getTaxRate())
                                                .build()))
                        .build();
        created.add(purchaseInvoiceService.createPurchaseInvoice(pi1));

        PurchaseInvoiceDto pi2 =
                PurchaseInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(EIGHT_DAYS))
                        .partyId(vendor2.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.abaya.getId())
                                                .quantity(new BigDecimal("20"))
                                                .unitPrice(new BigDecimal("1600"))
                                                .taxRate(items.abaya.getTaxRate())
                                                .build(),
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.niqab.getId())
                                                .quantity(new BigDecimal("60"))
                                                .unitPrice(new BigDecimal("250"))
                                                .taxRate(items.niqab.getTaxRate())
                                                .build(),
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.stole.getId())
                                                .quantity(new BigDecimal("80"))
                                                .unitPrice(new BigDecimal("180"))
                                                .taxRate(items.stole.getTaxRate())
                                                .build()))
                        .build();
        created.add(purchaseInvoiceService.createPurchaseInvoice(pi2));

        // Seed Purchase Today
        PurchaseInvoiceDto piToday =
                PurchaseInvoiceDto.builder()
                        .invoiceDate(LocalDate.now())
                        .partyId(vendor1.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        PurchaseInvoiceLineDto.builder()
                                                .itemId(items.kurta.getId())
                                                .quantity(new BigDecimal("5"))
                                                .unitPrice(new BigDecimal("700"))
                                                .taxRate(items.kurta.getTaxRate())
                                                .build()))
                        .build();
        created.add(purchaseInvoiceService.createPurchaseInvoice(piToday));
        return created;
    }

    private List<SalesInvoiceDto> seedSalesInvoices(
            Warehouse mainWh, Party walkIn, Party ahmed, Party sara, Items items) {
        java.util.ArrayList<SalesInvoiceDto> created = new java.util.ArrayList<>();
        SalesInvoiceDto si1 =
                SalesInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(FIVE_DAYS))
                        .partyId(walkIn.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.kurta.getId())
                                                .quantity(new BigDecimal("3"))
                                                .unitPrice(items.kurta.getUnitPrice())
                                                .taxRate(items.kurta.getTaxRate())
                                                .build(),
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.hijab.getId())
                                                .quantity(new BigDecimal("5"))
                                                .unitPrice(items.hijab.getUnitPrice())
                                                .taxRate(items.hijab.getTaxRate())
                                                .build()))
                        .build();
        created.add(salesInvoiceService.createSalesInvoice(si1));

        SalesInvoiceDto si2 =
                SalesInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(THREE_DAYS))
                        .partyId(ahmed.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.abaya.getId())
                                                .quantity(new BigDecimal("2"))
                                                .unitPrice(items.abaya.getUnitPrice())
                                                .taxRate(items.abaya.getTaxRate())
                                                .build(),
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.stole.getId())
                                                .quantity(new BigDecimal("4"))
                                                .unitPrice(items.stole.getUnitPrice())
                                                .taxRate(items.stole.getTaxRate())
                                                .build()))
                        .build();
        created.add(salesInvoiceService.createSalesInvoice(si2));

        SalesInvoiceDto si3 =
                SalesInvoiceDto.builder()
                        .invoiceDate(LocalDate.now().minusDays(ONE_DAY))
                        .partyId(sara.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.silkDM.getId())
                                                .quantity(new BigDecimal("3"))
                                                .unitPrice(items.silkDM.getUnitPrice())
                                                .taxRate(items.silkDM.getTaxRate())
                                                .build(),
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.niqab.getId())
                                                .quantity(new BigDecimal("6"))
                                                .unitPrice(items.niqab.getUnitPrice())
                                                .taxRate(items.niqab.getTaxRate())
                                                .build()))
                        .build();
        created.add(salesInvoiceService.createSalesInvoice(si3));

        // Seed Sales Today
        SalesInvoiceDto siToday =
                SalesInvoiceDto.builder()
                        .invoiceDate(LocalDate.now())
                        .partyId(walkIn.getId())
                        .warehouseId(mainWh.getId())
                        .lines(
                                List.of(
                                        SalesInvoiceLineDto.builder()
                                                .itemId(items.kurta.getId())
                                                .quantity(new BigDecimal("2"))
                                                .unitPrice(items.kurta.getUnitPrice())
                                                .taxRate(items.kurta.getTaxRate())
                                                .build()))
                        .build();
        created.add(salesInvoiceService.createSalesInvoice(siToday));
        return created;
    }

    private void seedReturnsAndAdjustments(
            Warehouse mainWh,
            Party vendor1,
            Party walkIn,
            Items items,
            List<PurchaseInvoiceDto> purchaseInvoices,
            List<SalesInvoiceDto> salesInvoices) {
        SalesInvoiceDto salesInvoice = salesInvoices.stream().findFirst().orElse(null);
        if (salesInvoice != null) {
            salesReturnService.createSalesReturn(
                    SalesReturnDto.builder()
                            .salesInvoiceId(salesInvoice.getId())
                            .returnDate(LocalDate.now())
                            .partyId(walkIn.getId())
                            .warehouseId(mainWh.getId())
                            .lines(
                                    List.of(
                                            SalesReturnLineDto.builder()
                                                    .itemId(items.kurta.getId())
                                                    .quantity(new BigDecimal("1"))
                                                    .unitPrice(items.kurta.getUnitPrice())
                                                    .taxRate(items.kurta.getTaxRate())
                                                    .build()))
                            .build());
        }

        PurchaseInvoiceDto purchaseInvoice = purchaseInvoices.stream().findFirst().orElse(null);
        if (purchaseInvoice != null) {
            purchaseReturnService.createPurchaseReturn(
                    PurchaseReturnDto.builder()
                            .purchaseInvoiceId(purchaseInvoice.getId())
                            .returnDate(LocalDate.now().minusDays(ONE_DAY))
                            .partyId(vendor1.getId())
                            .warehouseId(mainWh.getId())
                            .lines(
                                    List.of(
                                            PurchaseReturnLineDto.builder()
                                                    .itemId(items.hijab.getId())
                                                    .quantity(new BigDecimal("2"))
                                                    .unitPrice(new BigDecimal("200"))
                                                    .taxRate(items.hijab.getTaxRate())
                                                    .build()))
                            .build());
        }

        stockAdjustmentService.createAdjustment(
                StockAdjustmentDto.builder()
                        .adjustmentDate(LocalDate.now())
                        .warehouseId(mainWh.getId())
                        .itemId(items.stole.getId())
                        .quantity(new BigDecimal("-1"))
                        .reason("Damaged item")
                        .build());
    }

    private static class Items {
        private Item kurta;
        private Item abaya;
        private Item kidsSet;
        private Item cottonDM;
        private Item silkDM;
        private Item hijab;
        private Item niqab;
        private Item stole;

        public void setKurta(Item kurta) {
            this.kurta = kurta;
        }

        public void setAbaya(Item abaya) {
            this.abaya = abaya;
        }

        public void setKidsSet(Item kidsSet) {
            this.kidsSet = kidsSet;
        }

        public void setCottonDM(Item cottonDM) {
            this.cottonDM = cottonDM;
        }

        public void setSilkDM(Item silkDM) {
            this.silkDM = silkDM;
        }

        public void setHijab(Item hijab) {
            this.hijab = hijab;
        }

        public void setNiqab(Item niqab) {
            this.niqab = niqab;
        }

        public void setStole(Item stole) {
            this.stole = stole;
        }
    }
}
