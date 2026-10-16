package com.adsplatform.serving.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.serving.click.ClickResult.Outcome;
import com.adsplatform.serving.decision.AdDecision;
import com.adsplatform.serving.decision.AdDecisionSlot;
import com.adsplatform.serving.decision.AdDecisionSlotRepository;

class ClickServiceTest {

    private static final String TOKEN = "token-abc";
    private static final Instant NOW = Instant.parse("2026-10-15T20:00:00Z");
    private static final UUID DECISION_ID = UUID.fromString("7f3a0000-0000-0000-0000-000000000001");

    private AdDecisionSlotRepository slotRepository;
    private AdClickRepository clickRepository;
    private CampaignRepository campaignRepository;
    private CreativeRepository creativeRepository;
    private ClickService clickService;

    @BeforeEach
    void setUp() {
        slotRepository = mock(AdDecisionSlotRepository.class);
        clickRepository = mock(AdClickRepository.class);
        campaignRepository = mock(CampaignRepository.class);
        creativeRepository = mock(CreativeRepository.class);

        clickService = new ClickService(
                slotRepository,
                clickRepository,
                campaignRepository,
                creativeRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // A slot issued `age` ago, for campaign 10, creative 15, at $1.50.
    private void slotIssued(Duration age) {
        AdDecision decision = mock(AdDecision.class);
        when(decision.getDecisionId()).thenReturn(DECISION_ID);

        AdDecisionSlot slot = mock(AdDecisionSlot.class);
        when(slot.getDecision()).thenReturn(decision);
        when(slot.getCampaignId()).thenReturn(10L);
        when(slot.getCreativeId()).thenReturn(15L);
        when(slot.getPriceCpc()).thenReturn(new BigDecimal("1.50"));
        when(slot.getCreatedAt()).thenReturn(NOW.minus(age));

        when(slotRepository.findByClickToken(TOKEN)).thenReturn(Optional.of(slot));
        when(creativeRepository.findById(15L)).thenReturn(Optional.of(
                Creative.builder()
                        .id(15L)
                        .destinationUrl("https://homerefresh.example.com/kitchen")
                        .build()));
    }

    private Campaign campaign(String budget, String spent) {
        Campaign campaign = Campaign.builder()
                .id(10L)
                .name("Home Refresh")
                .advertiser("Home Refresh Co")
                .agency("Direct")
                .budget(new BigDecimal(budget))
                .flightStart(LocalDate.of(2026, 10, 15))
                .flightEnd(LocalDate.of(2026, 11, 15))
                .status(Campaign.CampaignStatus.ACTIVE)
                .build();
        campaign.setSpent(new BigDecimal(spent));
        when(campaignRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(campaign));
        return campaign;
    }

    private void firstClick() {
        when(clickRepository.insertIfAbsent(eq(TOKEN), any(), any(), any(), any())).thenReturn(1);
    }

    @Test
    void unknownTokenIsNotFoundAndNothingIsRecorded() {
        when(slotRepository.findByClickToken("made-up")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clickService.recordClick("made-up"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(clickRepository, never()).insertIfAbsent(anyString(), any(), any(), any(), any());
    }

    @Test
    void firstClickChargesThePriceAndRedirectsToTheCreative() {
        slotIssued(Duration.ofMinutes(5));
        firstClick();
        Campaign campaign = campaign("1000.00", "0.00");

        ClickResult result = clickService.recordClick(TOKEN);

        assertThat(result.outcome()).isEqualTo(Outcome.CHARGED);
        assertThat(result.chargedAmount()).isEqualByComparingTo("1.50");
        assertThat(result.destinationUrl()).isEqualTo("https://homerefresh.example.com/kitchen");
        assertThat(campaign.getSpent()).isEqualByComparingTo("1.50");
        verify(clickRepository).setChargedAmount(eq(TOKEN), eq(new BigDecimal("1.50")));
    }

    @Test
    void repeatedClickIsNotChargedAgain() {
        slotIssued(Duration.ofMinutes(5));
        when(clickRepository.insertIfAbsent(eq(TOKEN), any(), any(), any(), any())).thenReturn(0);

        ClickResult result = clickService.recordClick(TOKEN);

        assertThat(result.outcome()).isEqualTo(Outcome.DUPLICATE);
        assertThat(result.chargedAmount()).isEqualByComparingTo("0");
        verify(campaignRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void chargeIsCappedAtWhatIsLeftOfTheBudget() {
        slotIssued(Duration.ofMinutes(5));
        firstClick();
        Campaign campaign = campaign("1000.00", "999.90");

        ClickResult result = clickService.recordClick(TOKEN);

        assertThat(result.chargedAmount()).isEqualByComparingTo("0.10");
        assertThat(campaign.getSpent()).isEqualByComparingTo("1000.00");
    }

    @Test
    void exhaustedBudgetRecordsTheClickButChargesNothing() {
        slotIssued(Duration.ofMinutes(5));
        firstClick();
        Campaign campaign = campaign("1000.00", "1000.00");

        ClickResult result = clickService.recordClick(TOKEN);

        assertThat(result.outcome()).isEqualTo(Outcome.CHARGED);
        assertThat(result.chargedAmount()).isEqualByComparingTo("0");
        assertThat(campaign.getSpent()).isEqualByComparingTo("1000.00");
    }

    @Test
    void expiredTokenRedirectsWithoutRecordingOrCharging() {
        slotIssued(Duration.ofHours(25));

        ClickResult result = clickService.recordClick(TOKEN);

        assertThat(result.outcome()).isEqualTo(Outcome.EXPIRED);
        assertThat(result.destinationUrl()).isEqualTo("https://homerefresh.example.com/kitchen");
        verify(clickRepository, never()).insertIfAbsent(anyString(), any(), any(), any(), any());
        verify(campaignRepository, never()).findByIdForUpdate(any());
    }
}