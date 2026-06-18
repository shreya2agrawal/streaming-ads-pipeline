package com.adsplatform.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String INVENTORY_CACHE_PREFIX = "inventory:slots:";

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String ML_SERVICE_URL = "http://localhost:8000/predict";

    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAllOrderByFillRateDesc();
    }

    public Inventory getByShowId(String showId) {
        return inventoryRepository.findByShowId(showId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for show: " + showId));
    }

    public Inventory createInventory(Inventory inventory) {
        Inventory saved = inventoryRepository.save(inventory);
        cacheAvailableSlots(saved);
        log.info("Inventory created for show: {}", saved.getTitle());
        return saved;
    }

    @Transactional
    public Inventory allocateSlots(String showId, Long slotsRequested) {
        Inventory inventory = getByShowId(showId);
        if (inventory.getAvailableSlots() < slotsRequested) {
            throw new RuntimeException(
                    String.format("Insufficient inventory for %s. Available: %d, Requested: %d",
                            inventory.getTitle(),
                            inventory.getAvailableSlots(),
                            slotsRequested));
        }
        inventory.setAllocatedSlots(inventory.getAllocatedSlots() + slotsRequested);
        Inventory saved = inventoryRepository.save(inventory);
        cacheAvailableSlots(saved);
        log.info("Allocated {} slots for show: {}", slotsRequested, inventory.getTitle());
        return saved;
    }

    @Transactional
    public Inventory releaseSlots(String showId, Long slotsToRelease) {
        Inventory inventory = getByShowId(showId);
        long newAllocated = Math.max(0, inventory.getAllocatedSlots() - slotsToRelease);
        inventory.setAllocatedSlots(newAllocated);
        Inventory saved = inventoryRepository.save(inventory);
        cacheAvailableSlots(saved);
        log.info("Released {} slots for show: {}", slotsToRelease, inventory.getTitle());
        return saved;
    }

    public Long getAvailableSlotsFromCache(String showId) {
        String cached = redisTemplate.opsForValue()
                .get(INVENTORY_CACHE_PREFIX + showId);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        Inventory inventory = getByShowId(showId);
        cacheAvailableSlots(inventory);
        return inventory.getAvailableSlots();
    }

    public List<Inventory> getAvailableInventory(Long minimumSlots) {
        return inventoryRepository.findWithAvailableSlots(minimumSlots);
    }

    public InventoryStats getStats() {
        List<Inventory> all = inventoryRepository.findAll();
        long totalSlots = all.stream()
                .mapToLong(Inventory::getTotalSlots).sum();
        long allocatedSlots = all.stream()
                .mapToLong(Inventory::getAllocatedSlots).sum();
        long availableSlots = all.stream()
                .mapToLong(Inventory::getAvailableSlots).sum();
        double overallFillRate = totalSlots > 0
                ? (double) allocatedSlots / totalSlots * 100
                : 0.0;
        return new InventoryStats(
                totalSlots, allocatedSlots, availableSlots, overallFillRate);
    }

    private void cacheAvailableSlots(Inventory inventory) {
        redisTemplate.opsForValue().set(
                INVENTORY_CACHE_PREFIX + inventory.getShowId(),
                String.valueOf(inventory.getAvailableSlots()),
                Duration.ofMinutes(10));
    }

    public String getBidSignal(String showId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("show_id", showId);
            requestBody.put("banner_position", 0);
            requestBody.put("device_type", 1);
            requestBody.put("connection_type", 0);
            requestBody.put("audience_segment_1", 0);
            requestBody.put("audience_segment_2", 0);
            requestBody.put("audience_segment_3", 0);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(ML_SERVICE_URL, request, String.class);

            if (rawResponse.getStatusCode().is2xxSuccessful() && rawResponse.getBody() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                // Map<String, Object> body = mapper.readValue(rawResponse.getBody(),
                // Map.class);
                // String bidSignal = (String) body.get("bid_signal");
                // log.info("ML bid signal for show {}: {}", showId, bidSignal);
                // return bidSignal != null ? bidSignal : "BID_NORMAL";
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = mapper.readValue(
                            rawResponse.getBody(),
                            Map.class);
                    String bidSignal = (String) body.get("bid_signal");
                    log.info("ML bid signal for show {}: {}", showId, bidSignal);
                    return bidSignal != null ? bidSignal : "BID_NORMAL";

                } catch (Exception parseException) {
                    log.warn("Failed to parse ML response: {}",
                            parseException.getMessage());
                }
            }
        } catch (RestClientException e) {
            log.warn(
                    "ML service unavailable for show {}: {}. "
                            + "Defaulting to BID_NORMAL",
                    showId, e.getMessage());
        }
        return "BID_NORMAL";
    }

    public record InventoryStats(
            long totalSlots,
            long allocatedSlots,
            long availableSlots,
            double overallFillRate) {
    }
}
