package com.adsplatform.creative;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.dto.CreateCreativeRequest;
import com.adsplatform.creative.dto.CreativeResponse;
import com.adsplatform.creative.dto.UpdateCreativeRequest;

@ExtendWith(MockitoExtension.class)
class CreativeServiceTest {

    @Mock
    private CreativeRepository creativeRepository;

    @Mock
    private CampaignRepository campaignRepository;

    private CreativeService creativeService;

    @BeforeEach
    void setUp() {
        creativeService = new CreativeService(
                creativeRepository,
                campaignRepository);
    }

    @Test
    void shouldCreateCreativeAsDraftAndUnreviewed() {

        Campaign campaign = campaign(7L);

        CreateCreativeRequest request = new CreateCreativeRequest(
                7L,
                "Sports Pre-Roll",
                CreativeType.VIDEO,
                "https://cdn.example.com/sports.mp4",
                "https://advertiser.example.com",
                "Train smarter this summer",
                30);

        when(campaignRepository.findById(7L))
                .thenReturn(Optional.of(campaign));

        when(creativeRepository.save(any(Creative.class)))
                .thenAnswer(invocation -> {
                    Creative creative = invocation.getArgument(0);
                    creative.setId(101L);
                    return creative;
                });

        CreativeResponse response =
                creativeService.createCreative(request);

        assertEquals(101L, response.id());
        assertEquals(7L, response.campaignId());
        assertEquals("Sports Pre-Roll", response.name());
        assertEquals(CreativeType.VIDEO, response.creativeType());
        assertEquals(30, response.durationSeconds());
        assertEquals(CreativeStatus.DRAFT, response.status());
        assertEquals(
                CreativeSafetyStatus.UNREVIEWED,
                response.safetyStatus());
    }

    @Test
    void shouldAllowImageCreativeWithoutDuration() {

        Campaign campaign = campaign(7L);

        CreateCreativeRequest request = new CreateCreativeRequest(
                7L,
                "Homepage Banner",
                CreativeType.IMAGE,
                "https://cdn.example.com/banner.jpg",
                "https://advertiser.example.com",
                "New fall collection",
                null);

        when(campaignRepository.findById(7L))
                .thenReturn(Optional.of(campaign));

        when(creativeRepository.save(any(Creative.class)))
                .thenAnswer(invocation -> {
                    Creative creative = invocation.getArgument(0);
                    creative.setId(102L);
                    return creative;
                });

        CreativeResponse response =
                creativeService.createCreative(request);

        assertEquals(102L, response.id());
        assertEquals(CreativeType.IMAGE, response.creativeType());
        assertEquals(null, response.durationSeconds());
    }

    @Test
    void shouldRejectVideoCreativeWithoutDuration() {

        Campaign campaign = campaign(7L);

        CreateCreativeRequest request = new CreateCreativeRequest(
                7L,
                "Sports Pre-Roll",
                CreativeType.VIDEO,
                "https://cdn.example.com/sports.mp4",
                "https://advertiser.example.com",
                "Train smarter this summer",
                null);

        when(campaignRepository.findById(7L))
                .thenReturn(Optional.of(campaign));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> creativeService.createCreative(request));

        assertEquals(
                "Duration is required for video creatives",
                exception.getMessage());

        verify(creativeRepository, never())
                .save(any(Creative.class));
    }

    @Test
    void shouldRejectCreativeWhenCampaignDoesNotExist() {

        CreateCreativeRequest request = new CreateCreativeRequest(
                999L,
                "Sports Pre-Roll",
                CreativeType.VIDEO,
                "https://cdn.example.com/sports.mp4",
                "https://advertiser.example.com",
                "Train smarter this summer",
                30);

        when(campaignRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> creativeService.createCreative(request));

        assertEquals(
                "Campaign not found: 999",
                exception.getMessage());

        verify(creativeRepository, never())
                .save(any(Creative.class));
    }

    @Test
    void shouldGetCreativeById() {

        Creative creative = creative(
                101L,
                CreativeStatus.DRAFT,
                CreativeSafetyStatus.UNREVIEWED);

        when(creativeRepository.findById(101L))
                .thenReturn(Optional.of(creative));

        CreativeResponse response =
                creativeService.getCreative(101L);

        assertEquals(101L, response.id());
        assertEquals(7L, response.campaignId());
        assertEquals("Sports Pre-Roll", response.name());
    }

    @Test
    void shouldThrowWhenCreativeDoesNotExist() {

        when(creativeRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> creativeService.getCreative(999L));

        assertEquals(
                "Creative not found: 999",
                exception.getMessage());
    }

    @Test
    void shouldUpdateCreativeAndResetSafetyStatus() {

        Creative creative = creative(
                101L,
                CreativeStatus.ACTIVE,
                CreativeSafetyStatus.APPROVED);

        UpdateCreativeRequest request = new UpdateCreativeRequest(
                "Sports Pre-Roll V2",
                CreativeType.VIDEO,
                "https://cdn.example.com/sports-v2.mp4",
                "https://advertiser.example.com/new",
                "Updated creative content",
                15);

        when(creativeRepository.findById(101L))
                .thenReturn(Optional.of(creative));

        when(creativeRepository.save(any(Creative.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreativeResponse response =
                creativeService.updateCreative(101L, request);

        assertEquals("Sports Pre-Roll V2", response.name());
        assertEquals(
                "https://cdn.example.com/sports-v2.mp4",
                response.mediaUrl());
        assertEquals(
                "https://advertiser.example.com/new",
                response.destinationUrl());
        assertEquals(
                "Updated creative content",
                response.contentText());
        assertEquals(15, response.durationSeconds());

        assertEquals(
                CreativeSafetyStatus.UNREVIEWED,
                response.safetyStatus());

        assertEquals(
                CreativeStatus.ACTIVE,
                response.status());

        assertEquals(
                7L,
                response.campaignId());
    }

    @Test
    void shouldRejectUpdateWhenCreativeDoesNotExist() {

        UpdateCreativeRequest request = new UpdateCreativeRequest(
                "Updated Creative",
                CreativeType.IMAGE,
                "https://cdn.example.com/new.jpg",
                "https://advertiser.example.com",
                "Updated content",
                null);

        when(creativeRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> creativeService.updateCreative(
                                999L,
                                request));

        assertEquals(
                "Creative not found: 999",
                exception.getMessage());

        verify(creativeRepository, never())
                .save(any(Creative.class));
    }

    @Test
    void shouldRejectUpdateToVideoWithoutDuration() {

        Creative creative = creative(
                101L,
                CreativeStatus.DRAFT,
                CreativeSafetyStatus.UNREVIEWED);

        UpdateCreativeRequest request = new UpdateCreativeRequest(
                "Updated Video",
                CreativeType.VIDEO,
                "https://cdn.example.com/new.mp4",
                "https://advertiser.example.com",
                "Updated video content",
                null);

        when(creativeRepository.findById(101L))
                .thenReturn(Optional.of(creative));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> creativeService.updateCreative(
                                101L,
                                request));

        assertEquals(
                "Duration is required for video creatives",
                exception.getMessage());

        verify(creativeRepository, never())
                .save(any(Creative.class));
    }

    @Test
    void shouldUpdateCreativeStatusWithoutChangingSafetyStatus() {

        Creative creative = creative(
                101L,
                CreativeStatus.DRAFT,
                CreativeSafetyStatus.APPROVED);

        when(creativeRepository.findById(101L))
                .thenReturn(Optional.of(creative));

        when(creativeRepository.save(any(Creative.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreativeResponse response =
                creativeService.updateStatus(
                        101L,
                        CreativeStatus.ACTIVE);

        assertEquals(
                CreativeStatus.ACTIVE,
                response.status());

        assertEquals(
                CreativeSafetyStatus.APPROVED,
                response.safetyStatus());
    }

    @Test
    void shouldRejectStatusUpdateWhenCreativeDoesNotExist() {

        when(creativeRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> creativeService.updateStatus(
                                999L,
                                CreativeStatus.ACTIVE));

        assertEquals(
                "Creative not found: 999",
                exception.getMessage());

        verify(creativeRepository, never())
                .save(any(Creative.class));
    }

    @Test
    void shouldReturnAllCreatives() {

        Creative firstCreative = creative(
                101L,
                CreativeStatus.DRAFT,
                CreativeSafetyStatus.UNREVIEWED);

        Creative secondCreative = creative(
                102L,
                CreativeStatus.ACTIVE,
                CreativeSafetyStatus.APPROVED);

        secondCreative.setName("Homepage Banner");
        secondCreative.setCreativeType(CreativeType.IMAGE);
        secondCreative.setDurationSeconds(null);

        when(creativeRepository.findAll())
                .thenReturn(List.of(
                        firstCreative,
                        secondCreative));

        List<CreativeResponse> responses =
                creativeService.getAllCreatives();

        assertEquals(2, responses.size());
        assertEquals(101L, responses.get(0).id());
        assertEquals(102L, responses.get(1).id());
        assertEquals(
                CreativeStatus.ACTIVE,
                responses.get(1).status());
    }

    @Test
    void shouldReturnCreativesForCampaign() {

        Creative firstCreative = creative(
                101L,
                CreativeStatus.DRAFT,
                CreativeSafetyStatus.UNREVIEWED);

        Creative secondCreative = creative(
                102L,
                CreativeStatus.ACTIVE,
                CreativeSafetyStatus.APPROVED);

        when(creativeRepository.findByCampaign_Id(7L))
                .thenReturn(List.of(
                        firstCreative,
                        secondCreative));

        List<CreativeResponse> responses =
                creativeService.getCreativesByCampaign(7L);

        assertEquals(2, responses.size());
        assertEquals(7L, responses.get(0).campaignId());
        assertEquals(7L, responses.get(1).campaignId());
    }

    private Campaign campaign(Long id) {
        return Campaign.builder()
                .id(id)
                .build();
    }

    private Creative creative(
            Long id,
            CreativeStatus status,
            CreativeSafetyStatus safetyStatus) {

        return Creative.builder()
                .id(id)
                .campaign(campaign(7L))
                .name("Sports Pre-Roll")
                .creativeType(CreativeType.VIDEO)
                .mediaUrl("https://cdn.example.com/sports.mp4")
                .destinationUrl("https://advertiser.example.com")
                .contentText("Train smarter this summer")
                .durationSeconds(30)
                .status(status)
                .safetyStatus(safetyStatus)
                .build();
    }
}