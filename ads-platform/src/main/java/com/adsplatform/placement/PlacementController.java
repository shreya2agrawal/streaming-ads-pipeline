package com.adsplatform.placement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adsplatform.common.ApiResponse;
import com.adsplatform.placement.dto.CreatePlacementRequest;
import com.adsplatform.placement.dto.UpdatePlacementRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/placements")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementService placementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Placement>>> getAllPlacements() {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.getAllPlacements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Placement>> getPlacementById(@PathVariable Long id) {
        return placementService.getPlacementById(id)
                .map(placement -> ResponseEntity.ok(ApiResponse.ok(placement)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Placement>>> getActivePlacements() {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.getActivePlacements()));
    }

    @GetMapping("/active/type/{type}")
    public ResponseEntity<ApiResponse<List<Placement>>> getActivePlacementsByType(@PathVariable PlacementType type) {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.getActivePlacementsByType(type)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Placement>> createPlacement(
            @Valid @RequestBody CreatePlacementRequest request) {
        Placement createdPlacement = placementService.createPlacement(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(createdPlacement));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<Placement>> updatePlacementActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return placementService.updatePlacementActiveStatus(id, active)
                .map(placement -> ResponseEntity.ok(ApiResponse.ok(placement)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Placement>> updatePlacement(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlacementRequest request) {
        return placementService.updatePlacement(id, request)
                .map(placement -> ResponseEntity.ok(ApiResponse.ok(placement)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
