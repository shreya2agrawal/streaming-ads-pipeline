package com.adsplatform.serving.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.adsplatform.campaign.Campaign.CampaignStatus;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.placement.PlacementRepository;
import com.adsplatform.serving.decision.DecisionOutcome.NoFillReason;
import com.adsplatform.serving.decision.dto.AdDecisionRequest;
import com.adsplatform.serving.decision.dto.AdDecisionResponse;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdServingServiceTest {

    @Mock
    private AdDecisionService adDecisionService;

    @Mock
    private AdDecisionRepository adDecisionRepository;

    @Mock
    private CreativeRepository creativeRepository;

    @Mock
    private PlacementRepository placementRepository;

    @InjectMocks
    private AdServingService adServingService;

    @BeforeEach
    void setUp() {
        when(placementRepository.existsById(1L)).thenReturn(true);
        when(adDecisionRepository.save(any(AdDecision.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static AdDecisionRequest request(String requestId, int slots) {
        return new AdDecisionRequest(
                requestId, 1L, DeviceType.TV, "show-101",
                "viewer-1", Set.of(), slots, null);
    }

    private static RankedAd ranked(long campaignId, long creativeId, String bid, double score) {
        CandidateAd candidate = new CandidateAd(
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
        return new RankedAd(candidate, 0.02, score);
    }

    private static Creative creative(long id) {
        return Creative.builder()
                .id(id)
                .name("Creative " + id)
                .creativeType(CreativeType.VIDEO)
                .mediaUrl("https://cdn.example.com/" + id + ".mp4")
                .destinationUrl("https://advertiser.example.com/" + id)
                .contentText("Ad text")
                .durationSeconds(15)
                .status(CreativeStatus.ACTIVE)
                .safetyStatus(CreativeSafetyStatus.APPROVED)
                .build();
    }

    private void twoWinners() {
        when(adDecisionService.decide(any())).thenReturn(new DecisionOutcome(
                List.of(ranked(1, 11, "1.50", 0.03), ranked(4, 20, "2.10", 0.02)),
                List.of(new Exclusion(7L, 30L,
                        IneligibilityReason.CREATIVE_TOO_LONG,
                        "Creative is 45s but placement allows up to 30s")),
                3,
                null));
        when(creativeRepository.findAllById(anyIterable()))
                .thenReturn(List.of(creative(11), creative(20)));
    }

    @Test
    void unknownPlacementIsNotFoundAndNothingIsDecided() {
        when(placementRepository.existsById(99L)).thenReturn(false);
        AdDecisionRequest unknown = new AdDecisionRequest(
                null, 99L, DeviceType.TV, "show-101", null, null, 1, null);

        assertThatThrownBy(() -> adServingService.serve(unknown, false))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(adDecisionService, never()).decide(any());
        verify(adDecisionRepository, never()).save(any());
    }

    @Test
    void savesDecisionWithNumberedSlotsAndUniqueTokens() {
        twoWinners();

        adServingService.serve(request("req-42", 2), false);

        ArgumentCaptor<AdDecision> saved = ArgumentCaptor.forClass(AdDecision.class);
        verify(adDecisionRepository).save(saved.capture());
        AdDecision decision = saved.getValue();

        assertThat(decision.getRequestId()).isEqualTo("req-42");
        assertThat(decision.getFilledSlots()).isEqualTo(2);
        assertThat(decision.getSlots()).extracting(AdDecisionSlot::getSlotPosition)
                .containsExactly(1, 2);
        assertThat(decision.getSlots()).extracting(AdDecisionSlot::getPriceCpc)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1.50"), new BigDecimal("2.10"));

        List<String> tokens = decision.getSlots().stream()
                .map(AdDecisionSlot::getClickToken).toList();
        assertThat(tokens).doesNotHaveDuplicates()
                .allSatisfy(token -> assertThat(token).hasSize(43));
    }

    @Test
    void responseHasMediaAndClickUrlButNotTheDestination() {
        twoWinners();

        AdDecisionResponse response = adServingService.serve(request("req-42", 2), false);

        assertThat(response.ads()).hasSize(2);
        AdDecisionResponse.ServedAd first = response.ads().get(0);
        assertThat(first.slot()).isEqualTo(1);
        assertThat(first.mediaUrl()).isEqualTo("https://cdn.example.com/11.mp4");
        assertThat(first.clickUrl()).startsWith("/api/v1/clicks/");
        assertThat(first.toString()).doesNotContain("advertiser.example.com");
    }

    @Test
    void explanationOnlyInDebugMode() {
        twoWinners();

        AdDecisionResponse normal = adServingService.serve(request("req-1", 2), false);
        AdDecisionResponse debug = adServingService.serve(request("req-2", 2), true);

        assertThat(normal.explanation()).isNull();
        assertThat(debug.explanation().candidateCount()).isEqualTo(3);
        assertThat(debug.explanation().excluded())
                .extracting(Exclusion::reasonCode)
                .containsExactly(IneligibilityReason.CREATIVE_TOO_LONG);
        assertThat(debug.explanation().winners())
                .extracting(AdDecisionResponse.ScoredAd::campaignId)
                .containsExactly(1L, 4L);
    }

    @Test
    void noFillIsRecordedWithItsReason() {
        when(adDecisionService.decide(any())).thenReturn(new DecisionOutcome(
                List.of(), List.of(), 0, NoFillReason.NO_CANDIDATES));
        when(creativeRepository.findAllById(anyIterable())).thenReturn(List.of());

        AdDecisionResponse response = adServingService.serve(request("req-7", 2), false);

        ArgumentCaptor<AdDecision> saved = ArgumentCaptor.forClass(AdDecision.class);
        verify(adDecisionRepository).save(saved.capture());
        assertThat(saved.getValue().getNoFillReason()).isEqualTo(NoFillReason.NO_CANDIDATES);
        assertThat(saved.getValue().getSlots()).isEmpty();

        assertThat(response.ads()).isEmpty();
        assertThat(response.noFillReason()).isEqualTo("NO_CANDIDATES");
    }

    @Test
    void missingRequestIdIsGenerated() {
        when(adDecisionService.decide(any())).thenReturn(new DecisionOutcome(
                List.of(), List.of(), 0, NoFillReason.NO_CANDIDATES));
        when(creativeRepository.findAllById(anyIterable())).thenReturn(List.of());

        adServingService.serve(request(null, 1), false);

        ArgumentCaptor<AdRequestContext> context = ArgumentCaptor.forClass(AdRequestContext.class);
        verify(adDecisionService).decide(context.capture());
        assertThat(context.getValue().requestId()).isNotBlank();
    }
}