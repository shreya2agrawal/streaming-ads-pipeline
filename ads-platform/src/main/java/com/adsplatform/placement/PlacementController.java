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
import com.adsplatform.placement.dto.PlacementResponse;
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
    public ResponseEntity<ApiResponse<List<PlacementResponse>>> getAllPlacements() {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.getAllPlacements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlacementResponse>> getPlacementById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.getPlacementById(id)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PlacementResponse>>> getActivePlacements() {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.getActivePlacements()));
    }

    @GetMapping("/active/type/{type}")
    public ResponseEntity<ApiResponse<List<PlacementResponse>>> getActivePlacementsByType(@PathVariable PlacementType type) {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.getActivePlacementsByType(type)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlacementResponse>> createPlacement(
            @Valid @RequestBody CreatePlacementRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(placementService.createPlacement(request)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<PlacementResponse>> updatePlacementActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.updatePlacementActiveStatus(id, active)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlacementResponse>> updatePlacement(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlacementRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok(placementService.updatePlacement(id, request)));
    }
}
