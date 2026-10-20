package com.adsplatform.serving.impression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.serving.decision.AdDecision;
import com.adsplatform.serving.decision.AdDecisionSlot;
import com.adsplatform.serving.decision.AdDecisionSlotRepository;
import com.adsplatform.serving.impression.ImpressionService.ImpressionOutcome;

class ImpressionServiceTest {

    private static final String TOKEN = "token-abc";
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private static final UUID DECISION_ID = UUID.fromString("7f3a0000-0000-0000-0000-000000000001");

    private AdDecisionSlotRepository slotRepository;
    private AdImpressionRepository impressionRepository;
    private ImpressionService impressionService;

    @BeforeEach
    void setUp() {
        slotRepository = mock(AdDecisionSlotRepository.class);
        impressionRepository = mock(AdImpressionRepository.class);
        impressionService = new ImpressionService(
                slotRepository,
                impressionRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // A slot issued `age` ago, for campaign 10, creative 15.
    private void slotIssued(Duration age) {
        AdDecision decision = mock(AdDecision.class);
        when(decision.getDecisionId()).thenReturn(DECISION_ID);

        AdDecisionSlot slot = mock(AdDecisionSlot.class);
        when(slot.getDecision()).thenReturn(decision);
        when(slot.getCampaignId()).thenReturn(10L);
        when(slot.getCreativeId()).thenReturn(15L);
        when(slot.getCreatedAt()).thenReturn(NOW.minus(age));

        when(slotRepository.findByClickToken(TOKEN)).thenReturn(Optional.of(slot));
    }

    @Test
    void unknownTokenIsNotFound() {
        when(slotRepository.findByClickToken("made-up")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> impressionService.recordImpression("made-up"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(impressionRepository, never()).insertIfAbsent(anyString(), any(), any(), any(), any());
    }

    @Test
    void firstReportIsRecordedWithTheSlotsDetails() {
        slotIssued(Duration.ofSeconds(5));
        when(impressionRepository.insertIfAbsent(eq(TOKEN), any(), any(), any(), any())).thenReturn(1);

        ImpressionOutcome outcome = impressionService.recordImpression(TOKEN);

        assertThat(outcome).isEqualTo(ImpressionOutcome.RECORDED);
        verify(impressionRepository).insertIfAbsent(TOKEN, DECISION_ID, 10L, 15L, NOW);
    }

    @Test
    void repeatedReportIsADuplicate() {
        slotIssued(Duration.ofSeconds(5));
        when(impressionRepository.insertIfAbsent(eq(TOKEN), any(), any(), any(), any())).thenReturn(0);

        assertThat(impressionService.recordImpression(TOKEN))
                .isEqualTo(ImpressionOutcome.DUPLICATE);
    }

    @Test
    void reportForAVeryOldSlotIsNotRecorded() {
        slotIssued(Duration.ofHours(25));

        assertThat(impressionService.recordImpression(TOKEN))
                .isEqualTo(ImpressionOutcome.EXPIRED);

        verify(impressionRepository, never()).insertIfAbsent(anyString(), any(), any(), any(), any());
    }

    @Test
    void justInsideTheLifetimeIsStillRecorded() {
        slotIssued(Duration.ofHours(23));
        when(impressionRepository.insertIfAbsent(eq(TOKEN), any(), any(), any(), any())).thenReturn(1);

        assertThat(impressionService.recordImpression(TOKEN))
                .isEqualTo(ImpressionOutcome.RECORDED);
    }
}