package com.adsplatform.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.adsplatform.placement.PlacementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class CampaignBiddingTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private PlacementRepository placementRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private CampaignService campaignService;

    private static Campaign campaign(BigDecimal bid) {
        return Campaign.builder()
                .name("Trail Series")
                .advertiser("Northpeak")
                .agency("Direct")
                .budget(new BigDecimal("1000.00"))
                .flightStart(LocalDate.of(2026, 10, 1))
                .flightEnd(LocalDate.of(2026, 11, 25))
                .bidCpc(bid)
                .build();
    }

    @Test
    void createStartsWithZeroSpendEvenIfClientSendsSpend() {
        Campaign request = campaign(new BigDecimal("1.50"));
        request.setSpent(new BigDecimal("999.00"));
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Campaign saved = campaignService.createCampaign(request);

        assertThat(saved.getSpent()).isEqualByComparingTo("0");
        assertThat(saved.getBidCpc()).isEqualByComparingTo("1.50");
    }

    @Test
    void createAllowsNoBid() {
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Campaign saved = campaignService.createCampaign(campaign(null));

        assertThat(saved.getBidCpc()).isNull();
    }

    @Test
    void createRejectsZeroBid() {
        assertThatThrownBy(() ->
                campaignService.createCampaign(campaign(new BigDecimal("0.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bid");

        verify(campaignRepository, never()).save(any());
    }

    @Test
    void updateWithoutBidKeepsExistingBid() {
        Campaign existing = campaign(new BigDecimal("1.50"));
        when(campaignRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Like the current UI: an edit that does not include a bid.
        Campaign saved = campaignService.updateCampaign(42L, campaign(null));

        assertThat(saved.getBidCpc()).isEqualByComparingTo("1.50");
    }

    @Test
    void updateWithNewBidChangesIt() {
        Campaign existing = campaign(new BigDecimal("1.50"));
        when(campaignRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Campaign saved = campaignService.updateCampaign(42L, campaign(new BigDecimal("2.25")));

        assertThat(saved.getBidCpc()).isEqualByComparingTo("2.25");
    }

    @Test
    void updateNeverCopiesSpendFromRequest() {
        Campaign existing = campaign(new BigDecimal("1.50"));
        existing.setSpent(new BigDecimal("300.00"));
        when(campaignRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Campaign request = campaign(null);
        request.setSpent(BigDecimal.ZERO);

        Campaign saved = campaignService.updateCampaign(42L, request);

        assertThat(saved.getSpent()).isEqualByComparingTo("300.00");
    }

    @Test
    void jsonRequestCannotSetSpend() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        Campaign parsed = objectMapper.readValue(
                "{\"name\":\"Trail Series\",\"spent\":999.00}",
                Campaign.class);

        assertThat(parsed.getSpent()).isEqualByComparingTo("0");
    }
}