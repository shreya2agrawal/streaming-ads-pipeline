package com.adsplatform.placement;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.common.exception.DuplicateResourceException;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.placement.dto.CreatePlacementRequest;
import com.adsplatform.placement.dto.PlacementResponse;
import com.adsplatform.placement.dto.UpdatePlacementRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlacementService {

    private final PlacementRepository placementRepository;

    @Transactional
    public PlacementResponse createPlacement(CreatePlacementRequest request) {

        String name = normalize(request.name());

        if (name != null && placementRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Placement name already exists: " + name);
        }

        Placement placement = Placement.create(
                request.name(),
                request.type(),
                request.maxAds(),
                request.maxAdDurationSeconds(),
                request.supportedDevices(),
                request.supportedCreativeTypes());
        return PlacementResponse.from(placementRepository.save(placement));
    }

    @Transactional(readOnly = true)
    public List<PlacementResponse> getAllPlacements() {
        return placementRepository.findAll()
                .stream()
                .map(PlacementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlacementResponse getPlacementById(Long id) {
        return PlacementResponse.from(findPlacement(id));
    }

    @Transactional(readOnly = true)
    public List<PlacementResponse> getActivePlacements() {
        return placementRepository.findByActiveTrue()
                .stream()
                .map(PlacementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlacementResponse> getActivePlacementsByType(PlacementType type) {
        return placementRepository.findByTypeAndActiveTrue(type)
                .stream()
                .map(PlacementResponse::from)
                .toList();
    }

    @Transactional
    public PlacementResponse updatePlacementActiveStatus(Long id, boolean active) {
        Placement placement = findPlacement(id);
        if (active) {
            placement.activate();
        } else {
            placement.deactivate();
        }
        return PlacementResponse.from(placement);
    }

    @Transactional
    public PlacementResponse updatePlacement(Long id, UpdatePlacementRequest request) {
        Placement placement = findPlacement(id);
        String name = normalize(request.name());

        if (name != null &&
                placementRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Placement name already exists: " + name);
        }

        placement.update(
                request.name(),
                request.type(),
                request.maxAds(),
                request.maxAdDurationSeconds(),
                request.supportedDevices(),
                request.supportedCreativeTypes());

        return PlacementResponse.from(placement);
    }

    private Placement findPlacement(Long id) {
        return placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found: " + id));
    }

    /*
     * Matches the entity's trimming, so "Pre-roll " and "Pre-roll"
     * are checked as the same name. A blank or null name is left
     * for the entity to reject with its own message.
     */
    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }
}
