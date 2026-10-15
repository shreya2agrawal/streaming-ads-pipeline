package com.adsplatform.serving.model;

import java.time.Instant;
import java.util.Set;

import com.adsplatform.placement.DeviceType;

/*
 * Represents one real-time ad opportunity.
 *
 * Example:
 * A viewer is watching show-101 on a TABLET
 * and the player reaches placement 5.
 */
public record AdRequestContext(
        String requestId,
        Long placementId,
        DeviceType deviceType,
        String showId,
        String viewerTrackId,
        Set<String> audienceSegments,
        int availableSlots,
        Instant requestedAt) {
    public AdRequestContext {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request ID is required");
        }
        if (placementId == null || placementId <= 0) {
            throw new IllegalArgumentException("Valid Placement ID is required");
        }
        if (deviceType == null) {
            throw new IllegalArgumentException("Device type is required");
        }
        if (showId == null || showId.isBlank()) {
            throw new IllegalArgumentException("Show ID is required");
        }
        if (availableSlots <= 0) {
            throw new IllegalArgumentException("Available slots must be a positive number");
        }

        audienceSegments = audienceSegments == null
                ? Set.of()
                : Set.copyOf(audienceSegments);

        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
    }
}
