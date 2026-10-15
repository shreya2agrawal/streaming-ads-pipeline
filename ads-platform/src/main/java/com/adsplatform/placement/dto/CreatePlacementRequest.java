package com.adsplatform.placement.dto;

import java.util.Set;

import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.placement.PlacementType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePlacementRequest(

    @NotBlank(message = "Placement name is required")
    String name,
    
    @NotNull(message = "Placement type is required")
    PlacementType type,

    @NotNull (message = "Max Ads is required")
    @Positive(message = "Max Ads must be greater than zero")
    Integer maxAds,

    @Positive(message = "Max ad duration seconds must be greater than zero")
    Integer maxAdDurationSeconds,

    @NotEmpty(message = "Placement must support at least one device")
    Set<DeviceType> supportedDevices,

    @NotEmpty(message = "Placement must support at least one creative type")
    Set<CreativeType> supportedCreativeTypes) {
    
}
