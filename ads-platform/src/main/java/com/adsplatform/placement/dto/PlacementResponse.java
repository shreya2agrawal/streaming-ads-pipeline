package com.adsplatform.placement.dto;

import java.util.List;

import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.placement.Placement;
import com.adsplatform.placement.PlacementType;

/*
 * API view of a Placement. Built inside the service transaction so
 * lazy collections are read while the session is still open.
 */
public record PlacementResponse(
    Long id,
    String name,
    PlacementType type,
    boolean active,
    Integer maxAds,
    Integer maxAdDurationSeconds,
    List<DeviceType> supportedDevices,
    List<CreativeType> supportedCreativeTypes
) {

    public static PlacementResponse from(Placement placement) {
        return new PlacementResponse(
            placement.getId(),
            placement.getName(),
            placement.getType(),
            placement.isActive(),
            placement.getMaxAds(),
            placement.getMaxAdDurationSeconds(),
            placement.getSupportedDevices().stream().sorted().toList(),
            placement.getSupportedCreativeTypes().stream().sorted().toList()
        );
    }
    
}
