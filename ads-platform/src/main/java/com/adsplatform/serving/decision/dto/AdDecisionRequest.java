package com.adsplatform.serving.decision.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.adsplatform.placement.DeviceType;
import com.adsplatform.serving.model.AdRequestContext;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/*
 * What a player sends when a viewer reaches an ad opportunity.
 *
 * requestId:       optional; generated if missing. Lets a player
 *                  correlate its own logs with the decision.
 * availableSlots:  capped, so one request cannot ask for unlimited ads.
 * viewerTimeZone:  accepted now so the contract is ready for
 *                  viewer-local flight scheduling; not used yet.
 */
public record AdDecisionRequest(

        String requestId,

        @NotNull(message = "Placement id is required")
        Long placementId,

        @NotNull(message = "Device type is required")
        DeviceType deviceType,

        @NotBlank(message = "Show id is required")
        String showId,

        String viewerTrackId,

        Set<String> audienceSegments,

        @NotNull(message = "Available slots is required")
        @Min(value = 1, message = "Available slots must be at least 1")
        @Max(value = AdDecisionRequest.MAX_SLOTS,
                message = "Available slots must be at most " + AdDecisionRequest.MAX_SLOTS)
        Integer availableSlots,

        String viewerTimeZone) {

    public static final int MAX_SLOTS = 10;

    public AdRequestContext toContext(Instant requestedAt) {
        return new AdRequestContext(
                requestId == null || requestId.isBlank()
                        ? UUID.randomUUID().toString()
                        : requestId,
                placementId,
                deviceType,
                showId,
                viewerTrackId,
                audienceSegments == null ? Set.of() : audienceSegments,
                availableSlots,
                requestedAt);
    }
}