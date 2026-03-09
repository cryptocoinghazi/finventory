package com.finventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finventory.model.SequenceType;
import com.finventory.model.Warehouse;
import com.finventory.repository.DocumentSequenceRepository;
import com.finventory.repository.WarehouseRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SequenceConcurrencyTest {

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private DocumentSequenceRepository documentSequenceRepository;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        documentSequenceRepository.deleteAll();
        warehouseRepository.deleteAll();

        warehouse = Warehouse.builder()
                .name("Branch 1")
                .code("B1")
                .location("Test Location")
                .stateCode("MH")
                .build();
        warehouseRepository.save(warehouse);
    }

    @Test
    void shouldGenerateUniqueSequencesConcurrently() throws InterruptedException {
        int numberOfThreads = 10;
        int requestsPerThread = 5;
        int totalRequests = numberOfThreads * requestsPerThread;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        Set<String> generatedSequences = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < totalRequests; i++) {
            executorService.submit(() -> {
                try {
                    latch.await(); // Wait for signal
                    String sequence = sequenceGeneratorService.generateSequence(
                            SequenceType.SALES_INVOICE, warehouse, LocalDate.now());
                    generatedSequences.add(sequence);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.countDown(); // Start all threads
        executorService.shutdown();
        boolean finished = executorService.awaitTermination(60, TimeUnit.SECONDS);

        assertTrue(finished, "Tasks did not finish in time");
        assertEquals(totalRequests, generatedSequences.size(), "Should generate unique sequences");

        // Verify format
        for (String seq : generatedSequences) {
            // Expected format: FYxx-B1-S-0000xx
            // e.g. FY24-B1-S-000001 (if today is 2024-03-xx, FY is 24)
            // or FY25-B1-S-000001 (if today is 2024-04-xx, FY is 25)
            assertTrue(seq.matches("FY\\d{2}-B1-S-\\d{6}"), "Sequence format invalid: " + seq);
        }
    }
}
