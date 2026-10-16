package com.adsplatform.serving.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adsplatform.campaign.Campaign.CampaignStatus;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.serving.candidate.CandidateSource;
import com.adsplatform.serving.decision.DecisionOutcome.NoFillReason;
import com.adsplatform.serving.eligibility.EligibilityEngine;
import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;
import com.adsplatform.serving.scoring.CtrScoringPort;

class AdDecisionServiceTest {

    private CandidateSource candidateSource;
    private EligibilityEngine eligibilityEngine;

    // pCTR per creative id; anything not listed gets 0.02.
    private final Map<Long, Double> ctrByCreative = new HashMap<>();

    private final CtrScoringPort stubScorer = (candidates, context) ->
            candidates.stream()
                    .map(candidate -> ctrByCreative.getOrDefault(candidate.creative().id(), 0.02))
                    .toList();

    private AdDecisionService service;

    @BeforeEach
    void setUp() {
        candidateSource = mock(CandidateSource.class);
        eligibilityEngine = mock(EligibilityEngine.class);
        service = new AdDecisionService(candidateSource, eligibilityEngine, stubScorer);

        // Eligible unless a test says otherwise.
        when(eligibilityEngine.evaluate(any(), any())).thenReturn(EligibilityResult.allowed());
    }

    private static AdRequestContext request(int slots) {
        return new AdRequestContext(
                "request-1", 1L, DeviceType.TV, "show-101",
                "viewer-1", Set.of(), slots, Instant.now());
    }

    private static CandidateAd candidate(long campaignId, long creativeId, String bid) {
        return new CandidateAd(
                new CandidateAd.CampaignSnapshot(
                        campaignId, "Campaign " + campaignId, CampaignStatus.ACTIVE,
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 25),
                        null, null, null, null, null, null, null,
                        new BigDecimal(bid)),
                new CandidateAd.CreativeSnapshot(
                        creativeId, CreativeType.VIDEO, CreativeStatus.ACTIVE,
                        CreativeSafetyStatus.APPROVED, 15),
                new CandidateAd.PlacementSnapshot(
                        1L, true, Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO), 30));
    }

    private static List<Long> winningCampaigns(DecisionOutcome outcome) {
        return outcome.winners().stream().map(RankedAd::campaignId).toList();
    }

    @Test
    void noCandidatesIsNoFill() {
        when(candidateSource.findCandidates(any())).thenReturn(List.of());

        DecisionOutcome outcome = service.decide(request(2));

        assertThat(outcome.filled()).isFalse();
        assertThat(outcome.noFillReason()).isEqualTo(NoFillReason.NO_CANDIDATES);
    }

    @Test
    void allIneligibleIsNoFillWithReasons() {
        CandidateAd halo = candidate(7, 30, "1.20");
        when(candidateSource.findCandidates(any())).thenReturn(List.of(halo));
        when(eligibilityEngine.evaluate(eq(halo), any())).thenReturn(
                EligibilityResult.ineligible(
                        IneligibilityReason.CREATIVE_TOO_LONG,
                        "Creative is 45s but placement allows up to 30s"));

        DecisionOutcome outcome = service.decide(request(2));

        assertThat(outcome.noFillReason()).isEqualTo(NoFillReason.NO_ELIGIBLE_CANDIDATES);
        assertThat(outcome.exclusions()).singleElement().satisfies(exclusion -> {
            assertThat(exclusion.campaignId()).isEqualTo(7L);
            assertThat(exclusion.reasonCode()).isEqualTo(IneligibilityReason.CREATIVE_TOO_LONG);
        });
    }

    @Test
    void ranksByBidTimesPctrNotByBidAlone() {
        CandidateAd lumen = candidate(4, 20, "2.10");      // 2.10 x 0.01 = 0.021
        CandidateAd northpeak = candidate(1, 11, "1.50");  // 1.50 x 0.05 = 0.075
        ctrByCreative.put(20L, 0.01);
        ctrByCreative.put(11L, 0.05);
        when(candidateSource.findCandidates(any())).thenReturn(List.of(lumen, northpeak));

        DecisionOutcome outcome = service.decide(request(2));

        assertThat(winningCampaigns(outcome)).containsExactly(1L, 4L);
        assertThat(outcome.winners().get(0).score()).isEqualTo(0.075, Offset.offset(1e-9));
    }

    @Test
    void keepsOnlyTheBestCreativePerCampaign() {
        CandidateAd weaker = candidate(1, 12, "1.50");
        CandidateAd stronger = candidate(1, 11, "1.50");
        CandidateAd other = candidate(4, 20, "1.00");
        ctrByCreative.put(12L, 0.03);
        ctrByCreative.put(11L, 0.05);
        when(candidateSource.findCandidates(any())).thenReturn(List.of(weaker, stronger, other));

        DecisionOutcome outcome = service.decide(request(2));

        assertThat(winningCampaigns(outcome)).containsExactly(1L, 4L);
        assertThat(outcome.winners().get(0).creativeId()).isEqualTo(11L);
    }

    @Test
    void fillsOnlyTheAvailableSlots() {
        when(candidateSource.findCandidates(any())).thenReturn(List.of(
                candidate(1, 11, "3.00"),
                candidate(2, 21, "2.00"),
                candidate(3, 31, "1.00")));

        DecisionOutcome outcome = service.decide(request(2));

        assertThat(winningCampaigns(outcome)).containsExactly(1L, 2L);
    }

    @Test
    void fewerCandidatesThanSlotsFillsWhatExists() {
        when(candidateSource.findCandidates(any())).thenReturn(List.of(candidate(1, 11, "1.50")));

        DecisionOutcome outcome = service.decide(request(3));

        assertThat(outcome.winners()).hasSize(1);
        assertThat(outcome.noFillReason()).isNull();
    }

    @Test
    void tiesGoToTheLowerCampaignId() {
        when(candidateSource.findCandidates(any())).thenReturn(List.of(
                candidate(9, 91, "1.50"),
                candidate(2, 21, "1.50")));

        DecisionOutcome outcome = service.decide(request(1));

        assertThat(winningCampaigns(outcome)).containsExactly(2L);
    }

    @Test
    void excludedAndEligibleAreBothReported() {
        CandidateAd eligible = candidate(1, 11, "1.50");
        CandidateAd excluded = candidate(4, 20, "2.10");
        when(candidateSource.findCandidates(any())).thenReturn(List.of(eligible, excluded));
        when(eligibilityEngine.evaluate(eq(excluded), any())).thenReturn(
                EligibilityResult.ineligible(
                        IneligibilityReason.DEVICE_NOT_TARGETED,
                        "Campaign does not target requested device"));

        DecisionOutcome outcome = service.decide(request(2));

        assertThat(winningCampaigns(outcome)).containsExactly(1L);
        assertThat(outcome.exclusions()).extracting(Exclusion::campaignId).containsExactly(4L);
        assertThat(outcome.candidateCount()).isEqualTo(2);
    }

    @Test
    void scorerReturningWrongCountIsABug() {
        when(candidateSource.findCandidates(any())).thenReturn(List.of(
                candidate(1, 11, "1.50"),
                candidate(2, 21, "1.50")));
        AdDecisionService brokenScorer = new AdDecisionService(
                candidateSource, eligibilityEngine, (candidates, context) -> List.of(0.02));

        assertThatThrownBy(() -> brokenScorer.decide(request(2)))
                .isInstanceOf(IllegalStateException.class);
    }
}