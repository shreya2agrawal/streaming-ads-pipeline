package com.adsplatform.serving.eligibility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.adsplatform.campaign.Campaign.CampaignStatus;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.serving.eligibility.rules.CampaignStatusRule;
import com.adsplatform.serving.eligibility.rules.CreativeSafetyRule;
import com.adsplatform.serving.eligibility.rules.CreativeStatusRule;
import com.adsplatform.serving.eligibility.rules.FlightDateRule;
import com.adsplatform.serving.eligibility.rules.PlacementCompatibilityRule;
import com.adsplatform.serving.eligibility.rules.TargetingRule;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

class EligibilityRulesTest {

    @Test
    void activeCampaignShouldBeEligible() {

        CampaignStatusRule rule =
                new CampaignStatusRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        validContext()
                );

        assertTrue(result.eligible());
    }

    @Test
    void inactiveCampaignShouldBeRejected() {

        CandidateAd candidate =
                candidateWithCampaignStatus(
                        CampaignStatus.PAUSED
                );

        CampaignStatusRule rule =
                new CampaignStatusRule();

        EligibilityResult result =
                rule.evaluate(
                        candidate,
                        validContext()
                );

        assertFalse(result.eligible());
    }

    @Test
    void campaignInsideFlightWindowShouldBeEligible() {

        FlightDateRule rule =
                new FlightDateRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        validContext()
                );

        assertTrue(result.eligible());
    }

    @Test
    void campaignBeforeFlightStartShouldBeRejected() {

        CandidateAd candidate =
                candidateWithFlightDates(
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 30)
                );

        FlightDateRule rule =
                new FlightDateRule();

        EligibilityResult result =
                rule.evaluate(
                        candidate,
                        validContext()
                );

        assertFalse(result.eligible());
    }

    @Test
    void campaignAfterFlightEndShouldBeRejected() {

        CandidateAd candidate =
                candidateWithFlightDates(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                );

        FlightDateRule rule =
                new FlightDateRule();

        EligibilityResult result =
                rule.evaluate(
                        candidate,
                        validContext()
                );

        assertFalse(result.eligible());
    }

    @Test
    void compatiblePlacementShouldBeEligible() {

        PlacementCompatibilityRule rule =
                new PlacementCompatibilityRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        validContext()
                );

        assertTrue(result.eligible());
    }

    @Test
    void differentPlacementShouldBeRejected() {

        AdRequestContext context =
                contextWithPlacement(
                        99L
                );

        PlacementCompatibilityRule rule =
                new PlacementCompatibilityRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        context
                );

        assertFalse(result.eligible());
    }

    @Test
    void unsupportedDeviceShouldBeRejected() {

        AdRequestContext context =
                new AdRequestContext(
                        "request-1",
                        10L,
                        DeviceType.TV,
                        "show-101",
                        "viewer-1",
                        Set.of("SPORTS_FANS"),
                        1,
                        Instant.parse(
                                "2026-10-13T16:00:00Z"
                        )
                );

        PlacementCompatibilityRule rule =
                new PlacementCompatibilityRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        context
                );

        assertFalse(result.eligible());
    }

    @Test
    void unsupportedCreativeTypeShouldBeRejected() {

        CandidateAd candidate =
                candidateWithCreativeType(
                        CreativeType.IMAGE
                );

        PlacementCompatibilityRule rule =
                new PlacementCompatibilityRule();

        EligibilityResult result =
                rule.evaluate(
                        candidate,
                        validContext()
                );

        assertFalse(result.eligible());
    }

    @Test
    void activeCreativeShouldBeEligible() {

        CreativeStatusRule rule =
                new CreativeStatusRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        validContext()
                );

        assertTrue(result.eligible());
    }

    @Test
    void inactiveCreativeShouldBeRejected() {

        CandidateAd candidate =
                candidateWithCreativeStatus(
                        CreativeStatus.PAUSED
                );

        CreativeStatusRule rule =
                new CreativeStatusRule();

        EligibilityResult result =
                rule.evaluate(
                        candidate,
                        validContext()
                );

        assertFalse(result.eligible());
    }

    @Test
    void approvedCreativeShouldBeEligible() {

        CreativeSafetyRule rule =
                new CreativeSafetyRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        validContext()
                );

        assertTrue(result.eligible());
    }

    @Test
    void unapprovedCreativeShouldBeRejected() {

        CandidateAd candidate =
                candidateWithSafetyStatus(
                        CreativeSafetyStatus.PENDING_REVIEW
                );

        CreativeSafetyRule rule =
                new CreativeSafetyRule();

        EligibilityResult result =
                rule.evaluate(
                        candidate,
                        validContext()
                );

        assertFalse(result.eligible());
    }

    @Test
    void matchingShowAndAudienceShouldBeEligible() {

        TargetingRule rule =
                new TargetingRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        validContext()
                );

        assertTrue(result.eligible());
    }

    @Test
    void nonMatchingShowShouldBeRejected() {

        AdRequestContext context =
                contextWithShow(
                        "show-999"
                );

        TargetingRule rule =
                new TargetingRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        context
                );

        assertFalse(result.eligible());
    }

    @Test
    void nonMatchingAudienceShouldBeRejected() {

        AdRequestContext context =
                contextWithAudience(
                        Set.of("GAMERS")
                );

        TargetingRule rule =
                new TargetingRule();

        EligibilityResult result =
                rule.evaluate(
                        validCandidate(),
                        context
                );

        assertFalse(result.eligible());
    }

    @Test
    void emptyCampaignTargetingShouldAllowRequest() {

        CandidateAd base =
                validCandidate();

        CandidateAd.CampaignSnapshot campaign =
                new CandidateAd.CampaignSnapshot(
                        base.campaign().id(),
                        base.campaign().name(),
                        base.campaign().status(),
                        base.campaign().flightStart(),
                        base.campaign().flightEnd(),
                        base.campaign().pacingPercent(),
                        base.campaign().impressionsDelivered(),
                        base.campaign().impressionsGoal(),
                        Set.of(),
                        Set.of(),
                        base.campaign().targetPlacementIds(),
                        base.campaign().targetDeviceTypes()
                );

        CandidateAd candidate =
                new CandidateAd(
                        campaign,
                        base.creative(),
                        base.placement()
                );

        TargetingRule rule =
                new TargetingRule();

        EligibilityResult result =
                rule.evaluate(
                        candidate,
                        validContext()
                );

        assertTrue(result.eligible());
    }

    @Test
    void eligibilityEngineShouldPassValidCandidate() {

        EligibilityEngine engine =
                new EligibilityEngine(
                        List.of(
                                new CampaignStatusRule(),
                                new FlightDateRule(),
                                new PlacementCompatibilityRule(),
                                new CreativeStatusRule(),
                                new CreativeSafetyRule(),
                                new TargetingRule()
                        )
                );

        EligibilityResult result =
                engine.evaluate(
                        validCandidate(),
                        validContext()
                );

        assertTrue(result.eligible());
    }

    @Test
    void eligibilityEngineShouldStopWhenRuleFails() {

        EligibilityEngine engine =
                new EligibilityEngine(
                        List.of(
                                new CampaignStatusRule(),
                                new FlightDateRule(),
                                new PlacementCompatibilityRule(),
                                new CreativeStatusRule(),
                                new CreativeSafetyRule(),
                                new TargetingRule()
                        )
                );

        CandidateAd candidate =
                candidateWithCampaignStatus(
                        CampaignStatus.PAUSED
                );

        EligibilityResult result =
                engine.evaluate(
                        candidate,
                        validContext()
                );

        assertFalse(result.eligible());
    }

    /*
     * -------------------------
     * Test fixtures
     * -------------------------
     */

    private CandidateAd validCandidate() {

        CandidateAd.CampaignSnapshot campaign =
                new CandidateAd.CampaignSnapshot(
                        1L,
                        "Sports Campaign",
                        CampaignStatus.ACTIVE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31),
                        100.0,
                        500L,
                        1000L,
                        Set.of("show-101"),
                        Set.of("SPORTS_FANS"),
                        Set.of(10L),
                        Set.of(
                                DeviceType.MOBILE,
                                DeviceType.TABLET
                        )
                );

        CandidateAd.CreativeSnapshot creative =
                new CandidateAd.CreativeSnapshot(
                        100L,
                        CreativeType.VIDEO,
                        CreativeStatus.ACTIVE,
                        CreativeSafetyStatus.APPROVED,
                        30
                );

        CandidateAd.PlacementSnapshot placement =
                new CandidateAd.PlacementSnapshot(
                        10L,
                        true,
                        Set.of(
                                DeviceType.MOBILE,
                                DeviceType.TABLET
                        ),
                        Set.of(
                                CreativeType.VIDEO
                        )
                );

        return new CandidateAd(
                campaign,
                creative,
                placement
        );
    }

    private AdRequestContext validContext() {

        return new AdRequestContext(
                "request-1",
                10L,
                DeviceType.TABLET,
                "show-101",
                "viewer-1",
                Set.of("SPORTS_FANS"),
                1,
                Instant.parse(
                        "2026-10-13T16:00:00Z"
                )
        );
    }

    private CandidateAd candidateWithCampaignStatus(
            CampaignStatus status) {

        CandidateAd base =
                validCandidate();

        CandidateAd.CampaignSnapshot campaign =
                new CandidateAd.CampaignSnapshot(
                        base.campaign().id(),
                        base.campaign().name(),
                        status,
                        base.campaign().flightStart(),
                        base.campaign().flightEnd(),
                        base.campaign().pacingPercent(),
                        base.campaign().impressionsDelivered(),
                        base.campaign().impressionsGoal(),
                        base.campaign().targetShowIds(),
                        base.campaign().targetingSegments(),
                        base.campaign().targetPlacementIds(),
                        base.campaign().targetDeviceTypes()
                );

        return new CandidateAd(
                campaign,
                base.creative(),
                base.placement()
        );
    }

    private CandidateAd candidateWithFlightDates(
            LocalDate flightStart,
            LocalDate flightEnd) {

        CandidateAd base =
                validCandidate();

        CandidateAd.CampaignSnapshot campaign =
                new CandidateAd.CampaignSnapshot(
                        base.campaign().id(),
                        base.campaign().name(),
                        base.campaign().status(),
                        flightStart,
                        flightEnd,
                        base.campaign().pacingPercent(),
                        base.campaign().impressionsDelivered(),
                        base.campaign().impressionsGoal(),
                        base.campaign().targetShowIds(),
                        base.campaign().targetingSegments(),
                        base.campaign().targetPlacementIds(),
                        base.campaign().targetDeviceTypes()
                );

        return new CandidateAd(
                campaign,
                base.creative(),
                base.placement()
        );
    }

    private CandidateAd candidateWithCreativeType(
            CreativeType creativeType) {

        CandidateAd base =
                validCandidate();

        CandidateAd.CreativeSnapshot creative =
                new CandidateAd.CreativeSnapshot(
                        base.creative().id(),
                        creativeType,
                        base.creative().creativeStatus(),
                        base.creative().creativeSafetyStatus(),
                        creativeType == CreativeType.VIDEO
                                ? 30
                                : null
                );

        return new CandidateAd(
                base.campaign(),
                creative,
                base.placement()
        );
    }

    private CandidateAd candidateWithCreativeStatus(
            CreativeStatus status) {

        CandidateAd base =
                validCandidate();

        CandidateAd.CreativeSnapshot creative =
                new CandidateAd.CreativeSnapshot(
                        base.creative().id(),
                        base.creative().creativeType(),
                        status,
                        base.creative().creativeSafetyStatus(),
                        base.creative().durationSeconds()
                );

        return new CandidateAd(
                base.campaign(),
                creative,
                base.placement()
        );
    }

    private CandidateAd candidateWithSafetyStatus(
            CreativeSafetyStatus safetyStatus) {

        CandidateAd base =
                validCandidate();

        CandidateAd.CreativeSnapshot creative =
                new CandidateAd.CreativeSnapshot(
                        base.creative().id(),
                        base.creative().creativeType(),
                        base.creative().creativeStatus(),
                        safetyStatus,
                        base.creative().durationSeconds()
                );

        return new CandidateAd(
                base.campaign(),
                creative,
                base.placement()
        );
    }

    private AdRequestContext contextWithPlacement(
            Long placementId) {

        AdRequestContext base =
                validContext();

        return new AdRequestContext(
                base.requestId(),
                placementId,
                base.deviceType(),
                base.showId(),
                base.viewerTrackId(),
                base.audienceSegments(),
                base.availableSlots(),
                base.requestedAt()
        );
    }

    private AdRequestContext contextWithShow(
            String showId) {

        AdRequestContext base =
                validContext();

        return new AdRequestContext(
                base.requestId(),
                base.placementId(),
                base.deviceType(),
                showId,
                base.viewerTrackId(),
                base.audienceSegments(),
                base.availableSlots(),
                base.requestedAt()
        );
    }

    private AdRequestContext contextWithAudience(
            Set<String> audienceSegments) {

        AdRequestContext base =
                validContext();

        return new AdRequestContext(
                base.requestId(),
                base.placementId(),
                base.deviceType(),
                base.showId(),
                base.viewerTrackId(),
                audienceSegments,
                base.availableSlots(),
                base.requestedAt()
        );
    }
}