package com.adsplatform.inventory;

import com.adsplatform.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Inventory>>> getAll(){
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllInventory()));
    }

    @GetMapping("/{showId}")
    public ResponseEntity<ApiResponse<Inventory>> getByShowId(
            @PathVariable String showId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getByShowId(showId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Inventory>> create(
            @RequestBody Inventory inventory) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.createInventory(inventory)));
    }

    @PostMapping("/{showId}/allocate")
    public ResponseEntity<ApiResponse<Inventory>> allocate(
            @PathVariable String showId,
            @RequestParam Long slots
    ){
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.allocateSlots(showId, slots))
        );
    }

    @PostMapping("/{showId}/release")
    public ResponseEntity<ApiResponse<Inventory>> release(
            @PathVariable String showId,
            @RequestParam Long slots
    ){
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.releaseSlots(showId, slots)
        ));
    }

    @GetMapping("/{showId}/available-slots")
    public ResponseEntity<ApiResponse<Long>> availableSlots(
            @PathVariable String showId
    ){
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.getAvailableSlotsFromCache(showId)
        ));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Inventory>>> available(
            @RequestParam(defaultValue = "1") Long minimumSlots
    ){
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.getAvailableInventory(minimumSlots)
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<InventoryService.InventoryStats>> stats(){
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.getStats()
        ));
    }
}
