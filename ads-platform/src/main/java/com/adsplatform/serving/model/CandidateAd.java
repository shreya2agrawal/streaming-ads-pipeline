package com.adsplatform.serving.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import com.adsplatform.campaign.Campaign.CampaignStatus;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;

/*
 * Represents one Campaign + Creative + Placement combination
 * that may be eligible to serve for an AdRequestContext.
 *
 * This is a serving model, not a database entity.
 */
public record CandidateAd(
        CampaignSnapshot campaign,
        CreativeSnapshot creative,
        PlacementSnapshot placement) {

    public CandidateAd {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign snapshot is required");
        }
        if (creative == null) {
            throw new IllegalArgumentException("Creative snapshot is required");
        }
        if (placement == null) {
            throw new IllegalArgumentException("Placement snapshot is required");
        }
    }

    public record CampaignSnapshot(
            Long id,
            String name,
            CampaignStatus status,
            LocalDate flightStart,
            LocalDate flightEnd,
            Double pacingPercent,
            Long impressionsDelivered,
            Long impressionsGoal,
            Set<String> targetShowIds,
            Set<String> targetingSegments,
            Set<Long> targetPlacementIds,
            Set<DeviceType> targetDeviceTypes,
            BigDecimal bidCpc) {
        public CampaignSnapshot {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException(
                        "Valid Campaign ID is required");
            }

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Campaign name is required");
            }

            if (status == null) {
                throw new IllegalArgumentException(
                        "Campaign Status is required");
            }

            if (bidCpc != null && bidCpc.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Bid per click must be greater than zero");
            }

            targetShowIds = targetShowIds == null
                    ? Set.of()
                    : Set.copyOf(targetShowIds);

            targetingSegments = targetingSegments == null
                    ? Set.of()
                    : Set.copyOf(targetingSegments);

            targetPlacementIds = targetPlacementIds == null
                    ? Set.of()
                    : Set.copyOf(targetPlacementIds);

            targetDeviceTypes = targetDeviceTypes == null
                    ? Set.of()
                    : Set.copyOf(targetDeviceTypes);

        }

        // When there is no bid: bidCpc is null.
        public CampaignSnapshot(
                Long id,
                String name,
                CampaignStatus status,
                LocalDate flightStart,
                LocalDate flightEnd,
                Double pacingPercent,
                Long impressionsDelivered,
                Long impressionsGoal,
                Set<String> targetShowIds,
                Set<String> targetingSegments,
                Set<Long> targetPlacementIds,
                Set<DeviceType> targetDeviceTypes) {

            this(id, name, status, flightStart, flightEnd,
                    pacingPercent, impressionsDelivered, impressionsGoal,
                    targetShowIds, targetingSegments,
                    targetPlacementIds, targetDeviceTypes, null);
        }
    }

    /*
     * Creative information required during eligibility.
     */
    public record CreativeSnapshot(
            Long id,
            CreativeType creativeType,
            CreativeStatus creativeStatus,
            CreativeSafetyStatus creativeSafetyStatus,
            Integer durationSeconds) {
        public CreativeSnapshot {

            if (id == null || id <= 0) {
                throw new IllegalArgumentException(
                        "Valid Creative ID is required");
            }

            if (creativeType == null) {
                throw new IllegalArgumentException(
                        "Creative Type is required");
            }

            if (creativeStatus == null) {
                throw new IllegalArgumentException(
                        "Creative Status is required");
            }

            if (creativeSafetyStatus == null) {
                throw new IllegalArgumentException(
                        "Creative Safety Status is required");
            }

            /*
             * Duration is required only for video creatives.
             */
            if (creativeType == CreativeType.VIDEO
                    && (durationSeconds == null || durationSeconds <= 0)) {

                throw new IllegalArgumentException(
                        "Valid Creative Duration is required for VIDEO creatives");
            }
        }
    }

    /*
     * Placement information required during eligibility.
     */
    public record PlacementSnapshot(
            Long id,
            boolean active,
            Set<DeviceType> supportedDevices,
            Set<CreativeType> supportedCreativeTypes,
            Integer maxAdDurationSeconds) {

        public PlacementSnapshot {

            if (id == null || id <= 0) {
                throw new IllegalArgumentException(
                        "Valid Placement ID is required");
            }

            supportedDevices = supportedDevices == null
                    ? Set.of()
                    : Set.copyOf(supportedDevices);

            supportedCreativeTypes = supportedCreativeTypes == null
                    ? Set.of()
                    : Set.copyOf(supportedCreativeTypes);

            if (maxAdDurationSeconds != null && maxAdDurationSeconds <= 0) {
                throw new IllegalArgumentException(
                        "Max ad duration must be greater than zero");
            }
        }

        public PlacementSnapshot(
                Long id,
                boolean active,
                Set<DeviceType> supportedDevices,
                Set<CreativeType> supportedCreativeTypes) {

            this(id, active, supportedDevices, supportedCreativeTypes, null);
        }
    }

}
