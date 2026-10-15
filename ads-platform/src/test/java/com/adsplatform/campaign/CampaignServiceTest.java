package com.adsplatform.campaign;

import com.adsplatform.placement.DeviceType;
import com.adsplatform.placement.PlacementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private PlacementRepository placementRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(
                campaignRepository,
                placementRepository,
                kafkaTemplate
        );
    }

    private void stubSave() {
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Campaign buildCampaign() {
        return Campaign.builder()
                .name("Streaming Campaign")
                .advertiser("Example Advertiser")
                .agency("Example Agency")
                .budget(new BigDecimal("5000.00"))
                .flightStart(LocalDate.of(2026, 10, 1))
                .flightEnd(LocalDate.of(2026, 10, 31))
                .status(Campaign.CampaignStatus.DRAFT)
                .pacingPercent(0.0)
                .impressionsDelivered(0L)
                .impressionsGoal(10000L)
                .build();
    }

    @Test
    void createCampaignShouldSaveAllTargeting() {
        stubSave();

        Campaign campaign = buildCampaign();

        campaign.setTargetShowIds(Set.of("show-101", "show-204"));
        campaign.setTargetingSegments(Set.of("SPORTS_FANS", "GAMERS"));
        campaign.setTargetPlacementIds(Set.of(1L, 2L));
        campaign.setTargetDeviceTypes(Set.of(DeviceType.TV, DeviceType.MOBILE));

        when(placementRepository.existsById(1L)).thenReturn(true);
        when(placementRepository.existsById(2L)).thenReturn(true);

        Campaign saved = campaignService.createCampaign(campaign);

        assertEquals(
                Set.of("show-101", "show-204"),
                saved.getTargetShowIds()
        );

        assertEquals(
                Set.of("SPORTS_FANS", "GAMERS"),
                saved.getTargetingSegments()
        );

        assertEquals(
                Set.of(1L, 2L),
                saved.getTargetPlacementIds()
        );

        assertEquals(
                Set.of(DeviceType.TV, DeviceType.MOBILE),
                saved.getTargetDeviceTypes()
        );

        verify(campaignRepository).save(campaign);
        verify(kafkaTemplate).send(
                "campaign-events",
                "CREATED: " + saved.getId()
        );
    }

    @Test
    void createCampaignShouldAllowEmptyTargeting() {
        stubSave();

        Campaign campaign = buildCampaign();

        Campaign saved = campaignService.createCampaign(campaign);

        assertTrue(saved.getTargetShowIds().isEmpty());
        assertTrue(saved.getTargetingSegments().isEmpty());
        assertTrue(saved.getTargetPlacementIds().isEmpty());
        assertTrue(saved.getTargetDeviceTypes().isEmpty());

        verify(campaignRepository).save(campaign);
    }

    @Test
    void createCampaignShouldConvertNullTargetingToEmptySets() {
        stubSave();

        Campaign campaign = buildCampaign();

        campaign.setTargetShowIds(null);
        campaign.setTargetingSegments(null);
        campaign.setTargetPlacementIds(null);
        campaign.setTargetDeviceTypes(null);

        Campaign saved = campaignService.createCampaign(campaign);

        assertNotNull(saved.getTargetShowIds());
        assertNotNull(saved.getTargetingSegments());
        assertNotNull(saved.getTargetPlacementIds());
        assertNotNull(saved.getTargetDeviceTypes());

        assertTrue(saved.getTargetShowIds().isEmpty());
        assertTrue(saved.getTargetingSegments().isEmpty());
        assertTrue(saved.getTargetPlacementIds().isEmpty());
        assertTrue(saved.getTargetDeviceTypes().isEmpty());
    }

    @Test
    void createCampaignShouldRejectUnknownPlacement() {
        Campaign campaign = buildCampaign();
        campaign.setTargetPlacementIds(Set.of(999L));

        when(placementRepository.existsById(999L))
                .thenReturn(false);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> campaignService.createCampaign(campaign)
                );

        assertEquals(
                "Target placement not found: 999",
                exception.getMessage()
        );

        verify(campaignRepository, never())
                .save(any(Campaign.class));

        verify(kafkaTemplate, never())
                .send(anyString(), anyString());
    }

    @Test
    void updateCampaignShouldReplaceAllTargeting() {
        stubSave();

        Campaign existing = buildCampaign();
        existing.setId(10L);

        existing.setTargetShowIds(Set.of("old-show"));
        existing.setTargetingSegments(Set.of("OLD_SEGMENT"));
        existing.setTargetPlacementIds(Set.of(1L));
        existing.setTargetDeviceTypes(Set.of(DeviceType.WEB));

        Campaign updated = buildCampaign();

        updated.setTargetShowIds(Set.of("show-300"));
        updated.setTargetingSegments(Set.of("MOVIE_FANS"));
        updated.setTargetPlacementIds(Set.of(2L, 3L));
        updated.setTargetDeviceTypes(
                Set.of(DeviceType.TV, DeviceType.TABLET)
        );

        when(campaignRepository.findById(10L))
                .thenReturn(java.util.Optional.of(existing));

        when(placementRepository.existsById(2L))
                .thenReturn(true);

        when(placementRepository.existsById(3L))
                .thenReturn(true);

        Campaign saved =
                campaignService.updateCampaign(10L, updated);

        assertEquals(
                Set.of("show-300"),
                saved.getTargetShowIds()
        );

        assertEquals(
                Set.of("MOVIE_FANS"),
                saved.getTargetingSegments()
        );

        assertEquals(
                Set.of(2L, 3L),
                saved.getTargetPlacementIds()
        );

        assertEquals(
                Set.of(DeviceType.TV, DeviceType.TABLET),
                saved.getTargetDeviceTypes()
        );
    }

    @Test
    void updateCampaignShouldClearTargetingWhenNullIsProvided() {
        stubSave();

        Campaign existing = buildCampaign();
        existing.setId(10L);

        existing.setTargetShowIds(Set.of("show-101"));
        existing.setTargetingSegments(Set.of("GAMERS"));
        existing.setTargetPlacementIds(Set.of(1L));
        existing.setTargetDeviceTypes(Set.of(DeviceType.TV));

        Campaign updated = buildCampaign();

        updated.setTargetShowIds(null);
        updated.setTargetingSegments(null);
        updated.setTargetPlacementIds(null);
        updated.setTargetDeviceTypes(null);

        when(campaignRepository.findById(10L))
                .thenReturn(java.util.Optional.of(existing));

        Campaign saved =
                campaignService.updateCampaign(10L, updated);

        assertTrue(saved.getTargetShowIds().isEmpty());
        assertTrue(saved.getTargetingSegments().isEmpty());
        assertTrue(saved.getTargetPlacementIds().isEmpty());
        assertTrue(saved.getTargetDeviceTypes().isEmpty());
    }

    @Test
    void updateCampaignShouldRejectUnknownPlacement() {
        Campaign existing = buildCampaign();
        existing.setId(10L);

        Campaign updated = buildCampaign();
        updated.setTargetPlacementIds(Set.of(999L));

        when(campaignRepository.findById(10L))
                .thenReturn(java.util.Optional.of(existing));

        when(placementRepository.existsById(999L))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.updateCampaign(10L, updated)
        );

        verify(campaignRepository, never())
                .save(any(Campaign.class));

        verify(kafkaTemplate, never())
                .send(anyString(), anyString());
    }

    @Test
    void getCampaignShouldReturnTargetingInformation() {
        Campaign campaign = buildCampaign();
        campaign.setId(10L);

        campaign.setTargetShowIds(Set.of("show-101"));
        campaign.setTargetingSegments(Set.of("GAMERS"));
        campaign.setTargetPlacementIds(Set.of(1L));
        campaign.setTargetDeviceTypes(Set.of(DeviceType.TV));

        when(campaignRepository.findById(10L))
                .thenReturn(java.util.Optional.of(campaign));

        Campaign result =
                campaignService.getCampaignById(10L);

        assertEquals(
                Set.of("show-101"),
                result.getTargetShowIds()
        );

        assertEquals(
                Set.of("GAMERS"),
                result.getTargetingSegments()
        );

        assertEquals(
                Set.of(1L),
                result.getTargetPlacementIds()
        );

        assertEquals(
                Set.of(DeviceType.TV),
                result.getTargetDeviceTypes()
        );
    }
}