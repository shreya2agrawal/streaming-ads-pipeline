package com.adsplatform.placement;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.adsplatform.placement.dto.CreatePlacementRequest;
import com.adsplatform.placement.dto.UpdatePlacementRequest;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PlacementService {

    private final PlacementRepository placementRepository;

    public Placement createPlacement(CreatePlacementRequest request){
        Placement placement = Placement.builder()
        .name(request.name())
        .type(request.type())
        .maxAds(request.maxAds())
        .durationSeconds(request.durationSeconds())
        .supportedDevices(
            request.supportedDevices() == null 
            ? new HashSet<>()
            : new HashSet<>(request.supportedDevices())
        )
        .supportedCreativeTypes(
            request.supportedCreativeTypes() == null
            ? new HashSet<>()
            : new HashSet<>(request.supportedCreativeTypes())
        )
        .active(false)
        .build();
        return placementRepository.save(placement);
    }

    public List<Placement> getAllPlacements(){
        return placementRepository.findAll();
    }

    public Optional<Placement> getPlacementById(Long id){
        return placementRepository.findById(id);
    }

    public List<Placement> getActivePlacements(){
        return placementRepository.findByActiveTrue();
    }

    public List<Placement> getActivePlacementsByType(PlacementType type){
        return placementRepository.findByTypeAndActiveTrue(type);
    }

    public Optional<Placement> updatePlacementActiveStatus(Long id, boolean active){
        return placementRepository.findById(id)
        .map(placement -> {
            placement.setActive(active);
            return placementRepository.save(placement);
        });
    }

    public Optional<Placement> updatePlacement(Long id, UpdatePlacementRequest request){
        return placementRepository.findById(id)
        .map(placement -> {
            placement.setName(request.name());
            placement.setType(request.type());
            placement.setMaxAds(request.maxAds());
            placement.setDurationSeconds(request.durationSeconds());
            placement.setSupportedDevices(new HashSet<>(request.supportedDevices()));
            placement.setSupportedCreativeTypes(new HashSet<>(request.supportedCreativeTypes()));
            return placementRepository.save(placement);
        });
    }
    
}
