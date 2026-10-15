package com.adsplatform.serving.candidate;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.Campaign.CampaignStatus;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.placement.Placement;
import com.adsplatform.placement.PlacementRepository;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;
import com.adsplatform.serving.model.CandidateAd.CampaignSnapshot;
import com.adsplatform.serving.model.CandidateAd.CreativeSnapshot;
import com.adsplatform.serving.model.CandidateAd.PlacementSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * Reads candidates from PostgreSQL with one bounded query.
 *
 * Runs in a read-only transaction so lazy collections can be read
 * while building snapshots; batch fetching keeps that to a few
 * queries instead of several per campaign.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCandidateSource implements CandidateSource {

    /*
     * The exact flight check is FlightDateRule, in the platform time
     * zone. The query only removes campaigns clearly outside their
     * flight; one day either side covers every time zone.
     */
    static final int FLIGHT_MARGIN_DAYS = 1;

    private final PlacementRepository placementRepository;
    private final CreativeRepository creativeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CandidateAd> findCandidates(AdRequestContext context) {

        Optional<Placement> placement = placementRepository.findById(context.placementId());

        if (placement.isEmpty()) {
            log.debug("No candidates: unknown placement {}", context.placementId());
            return List.of();
        }

        PlacementSnapshot placementSnapshot = toSnapshot(placement.get());

        if (placementSnapshot.supportedCreativeTypes().isEmpty()) {
            return List.of();
        }

        LocalDate requestDay = LocalDate.ofInstant(context.requestedAt(), ZoneOffset.UTC);

        List<Creative> creatives = creativeRepository.findServableCreatives(
                CampaignStatus.ACTIVE,
                CreativeStatus.ACTIVE,
                CreativeSafetyStatus.APPROVED,
                requestDay.minusDays(FLIGHT_MARGIN_DAYS),
                requestDay.plusDays(FLIGHT_MARGIN_DAYS),
                placementSnapshot.supportedCreativeTypes());

        // Several creatives can share one campaign; build its snapshot once.
        Map<Long, CampaignSnapshot> campaignSnapshots = new HashMap<>();
        List<CandidateAd> candidates = new ArrayList<>(creatives.size());

        for (Creative creative : creatives) {
            try {
                CampaignSnapshot campaignSnapshot = campaignSnapshots.computeIfAbsent(
                        creative.getCampaign().getId(),
                        id -> toSnapshot(creative.getCampaign()));
                candidates.add(new CandidateAd(
                        campaignSnapshot,
                        toSnapshot(creative),
                        placementSnapshot));
            } catch (IllegalArgumentException invalidData) {
                // One bad row must not break serving for everyone else.
                log.warn("Skipping creative {}: {}",
                        creative.getId(), invalidData.getMessage());
            }
        }

        return candidates;
    }

    private static PlacementSnapshot toSnapshot(Placement placement) {
        return new PlacementSnapshot(
                placement.getId(),
                placement.isActive(),
                placement.getSupportedDevices(),
                placement.getSupportedCreativeTypes(),
                placement.getMaxAdDurationSeconds());
    }

    private static CampaignSnapshot toSnapshot(Campaign campaign) {
        return new CampaignSnapshot(
                campaign.getId(),
                campaign.getName(),
                campaign.getStatus(),
                campaign.getFlightStart(),
                campaign.getFlightEnd(),
                campaign.getPacingPercent(),
                campaign.getImpressionsDelivered(),
                campaign.getImpressionsGoal(),
                campaign.getTargetShowIds(),
                campaign.getTargetingSegments(),
                campaign.getTargetPlacementIds(),
                campaign.getTargetDeviceTypes(),
                campaign.getBidCpc());
    }

    private static CreativeSnapshot toSnapshot(Creative creative) {
        return new CreativeSnapshot(
                creative.getId(),
                creative.getCreativeType(),
                creative.getStatus(),
                creative.getSafetyStatus(),
                creative.getDurationSeconds());
    }

}
