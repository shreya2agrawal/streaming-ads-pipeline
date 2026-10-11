package com.adsplatform.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.dto.CreatePlacementRequest;
import com.adsplatform.placement.dto.UpdatePlacementRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PlacementServiceTest {

    @Mock
    private PlacementRepository placementRepository;

    @InjectMocks
    private PlacementService placementService;

    @Test
    void shouldActivateExistingPlacement() {
        Placement placement = Placement.builder()
                .id(1L)
                .name("Mid-Roll")
                .type(PlacementType.MID_ROLL)
                .active(false)
                .build();

        when(placementRepository.findById(1L))
                .thenReturn(Optional.of(placement));

        when(placementRepository.save(placement))
                .thenReturn(placement);

        Optional<Placement> result = placementService.updatePlacementActiveStatus(1L, true);

        assertTrue(result.isPresent());
        assertTrue(result.get().isActive());

        verify(placementRepository).findById(1L);
        verify(placementRepository).save(placement);
    }

    @Test
    void shouldReturnEmptyWhenPlacementDoesNotExist() {
        when(placementRepository.findById(99L))
                .thenReturn(Optional.empty());

        Optional<Placement> result = placementService.updatePlacementActiveStatus(99L, true);

        assertTrue(result.isEmpty());

        verify(placementRepository).findById(99L);
        verify(placementRepository, never()).save(any());
    }

    @Test
    void shouldUpdateExistingPlacement() {
        Placement placement = Placement.builder()
                .id(1L)
                .name("Old Mid-Roll")
                .type(PlacementType.MID_ROLL)
                .maxAds(1)
                .durationSeconds(15)
                .active(true)
                .supportedDevices(new HashSet<>(Set.of(DeviceType.TV)))
                .supportedCreativeTypes(new HashSet<>(Set.of(CreativeType.VIDEO)))
                .build();

        UpdatePlacementRequest request = new UpdatePlacementRequest(
                "Prime Mid-Roll",
                PlacementType.MID_ROLL,
                2,
                30,
                Set.of(DeviceType.TV, DeviceType.WEB),
                Set.of(CreativeType.VIDEO));

        when(placementRepository.findById(1L))
                .thenReturn(Optional.of(placement));

        when(placementRepository.save(placement))
                .thenReturn(placement);

        Optional<Placement> result = placementService.updatePlacement(1L, request);

        assertTrue(result.isPresent());
        assertEquals("Prime Mid-Roll", result.get().getName());
        assertEquals(2, result.get().getMaxAds());
        assertEquals(30, result.get().getDurationSeconds());
        assertTrue(result.get().isActive());

        verify(placementRepository).findById(1L);
        verify(placementRepository).save(placement);
    }

    @Test
    void shouldReturnEmptyWhenUpdatingMissingPlacement() {
        UpdatePlacementRequest request = new UpdatePlacementRequest(
                "Prime Mid-Roll",
                PlacementType.MID_ROLL,
                2,
                30,
                Set.of(DeviceType.TV),
                Set.of(CreativeType.VIDEO));

        when(placementRepository.findById(99L))
                .thenReturn(Optional.empty());

        Optional<Placement> result = placementService.updatePlacement(99L, request);

        assertTrue(result.isEmpty());

        verify(placementRepository).findById(99L);
        verify(placementRepository, never()).save(any());
    }

    @Test
    void shouldCreatePlacementAsInactive() {
        CreatePlacementRequest request = new CreatePlacementRequest(
                "Pre-Roll",
                PlacementType.PRE_ROLL,
                1,
                30,
                Set.of(DeviceType.TV, DeviceType.WEB),
                Set.of(CreativeType.VIDEO));

        when(placementRepository.save(any(Placement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Placement result = placementService.createPlacement(request);

        assertEquals("Pre-Roll", result.getName());
        assertEquals(PlacementType.PRE_ROLL, result.getType());
        assertEquals(1, result.getMaxAds());
        assertEquals(30, result.getDurationSeconds());
        assertTrue(result.getSupportedDevices().contains(DeviceType.TV));
        assertTrue(result.getSupportedDevices().contains(DeviceType.WEB));
        assertTrue(result.getSupportedCreativeTypes().contains(CreativeType.VIDEO));
        assertFalse(result.isActive());

        verify(placementRepository).save(any(Placement.class));
    }
}