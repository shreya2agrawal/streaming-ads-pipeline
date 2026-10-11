package com.adsplatform.placement.dto;

import java.util.Set;

import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.placement.PlacementType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdatePlacementRequest(
    @NotBlank(message = "Placement name is required")
    String name,
    @NotNull(message = "Placement type is required")
    PlacementType type,
    @Positive(message = "Max Ads must be greater than 0")
    Integer maxAds,
    @Positive(message = "Duration must be greater than 0")
    Integer durationSeconds,
    @NotEmpty(message = "At least one supported device is required")
    Set<DeviceType> supportedDevices,
    @NotEmpty(message = "At least one supported creative type is required")
    Set<CreativeType> supportedCreativeTypes
) {
}
