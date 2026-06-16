package com.adsplatform.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String INVENTORY_CACHE_PREFIX = "inventory:slots:";

    public List<Inventory> getAllInventory(){
        return inventoryRepository.findAllOrderByFillRateDesc();
    }

    public Inventory getByShowId(String showId){
        return inventoryRepository.findByShowId(showId)
                .orElseThrow(()
                        -> new RuntimeException("Inventory not found for show: " + showId));
    }

    public Inventory createInventory(Inventory inventory){
        Inventory saved = inventoryRepository.save(inventory);
        cacheAvailableSlots(saved);
        log.info("Inventory created for show: {}", saved.getTitle());
        return saved;
    }

    @Transactional
    public Inventory allocateSlots(String showId, Long slotsRequested){
        Inventory inventory = getByShowId(showId);
        if(inventory.getAvailableSlots() < slotsRequested){
            throw new RuntimeException(
                    String.format("Insufficient inventory for %s. Available: %d, Requested: %d",
                            inventory.getTitle(),
                            inventory.getAvailableSlots(),
                            slotsRequested)
            );
        }
        inventory.setAllocatedSlots(inventory.getAllocatedSlots() + slotsRequested);
        Inventory saved = inventoryRepository.save(inventory);
        cacheAvailableSlots(saved);
        log.info("Allocated {} slots for show: {}", slotsRequested, inventory.getTitle());
        return saved;
    }

    @Transactional
    public Inventory releaseSlots(String showId, Long slotsToRelease){
        Inventory inventory = getByShowId(showId);
        long newAllocated = Math.max(0, inventory.getAllocatedSlots() - slotsToRelease);
        inventory.setAllocatedSlots(newAllocated);
        Inventory saved = inventoryRepository.save(inventory);
        cacheAvailableSlots(saved);
        log.info("Released {} slots for show: {}", slotsToRelease, inventory.getTitle());
        return saved;
    }

    public Long getAvailableSlotsFromCache(String showId){
        String cached = redisTemplate.opsForValue()
                .get(INVENTORY_CACHE_PREFIX + showId);
        if(cached != null){
            return Long.parseLong(cached);
        }
        Inventory inventory = getByShowId(showId);
        cacheAvailableSlots(inventory);
        return inventory.getAvailableSlots();
    }

    public List<Inventory> getAvailableInventory(Long minimumSlots){
        return inventoryRepository.findWithAvailableSlots(minimumSlots);
    }

    public InventoryStats getStats(){
        List<Inventory> all = inventoryRepository.findAll();
        long totalSlots = all.stream()
                .mapToLong(Inventory::getTotalSlots).sum();
        long allocatedSlots = all.stream()
                .mapToLong(Inventory::getAllocatedSlots).sum();
        long availableSlots = all.stream()
                .mapToLong(Inventory::getAvailableSlots).sum();
        double overallFillRate = totalSlots > 0
                ? (double) allocatedSlots / totalSlots * 100 : 0.0;
        return new InventoryStats(
                totalSlots, allocatedSlots, availableSlots, overallFillRate
        );
    }

    private void cacheAvailableSlots(Inventory inventory){
        redisTemplate.opsForValue().set(
                INVENTORY_CACHE_PREFIX + inventory.getShowId(),
                String.valueOf(inventory.getAvailableSlots()),
                Duration.ofMinutes(10)
        );
    }

    public record InventoryStats(
            long totalSlots,
            long allocatedSlots,
            long availableSlots,
            double overallFillRate
    ){}
}
