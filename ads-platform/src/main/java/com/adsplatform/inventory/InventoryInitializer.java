package com.adsplatform.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class InventoryInitializer implements ApplicationRunner {

    private final InventoryRepository inventoryRepository;

    @Override
    public void run(ApplicationArguments args){
        if(inventoryRepository.count() > 0){
            log.info("Inventory already seeded, skipping.");
            return;
        }
        log.info("Seeding inventory data...");

        inventoryRepository.saveAll(List.of(
                Inventory.builder()
                        .showId("show-001").title("Stranger Things")
                        .genre("Sci-fi Drama").contentRating("TV-14")
                        .totalSlots(1_000_000L).allocatedSlots(920_000L)
                        .reservedSlots(0L).status(Inventory.InventoryStatus.NEAR_SOLD_OUT)
                        .build(),
                Inventory.builder()
                        .showId("show-002").title("Wednesday")
                        .genre("Comedy Horror").contentRating("TV-14")
                        .totalSlots(800_000L).allocatedSlots(592_000L)
                        .reservedSlots(0L).status(Inventory.InventoryStatus.AVAILABLE)
                        .build(),
                Inventory.builder()
                        .showId("show-003").title("The Crown")
                        .genre("Historical Drama").contentRating("TV-MA")
                        .totalSlots(600_000L).allocatedSlots(330_000L)
                        .reservedSlots(0L).status(Inventory.InventoryStatus.AVAILABLE)
                        .build(),
                Inventory.builder()
                        .showId("show-004").title("Bridgerton")
                        .genre("Romance Drama").contentRating("TV-MA")
                        .totalSlots(750_000L).allocatedSlots(607_500L)
                        .reservedSlots(0L).status(Inventory.InventoryStatus.NEAR_SOLD_OUT)
                        .build(),
                Inventory.builder()
                        .showId("show-005").title("Squid Game")
                        .genre("Thriller").contentRating("TV-MA")
                        .totalSlots(1_200_000L).allocatedSlots(1_164_000L)
                        .reservedSlots(0L).status(Inventory.InventoryStatus.NEAR_SOLD_OUT)
                        .build()
        ));

        log.info("Inventory seeding complete.");
    }
}
