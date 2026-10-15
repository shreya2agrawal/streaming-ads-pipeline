package com.adsplatform.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adsplatform.common.exception.DuplicateResourceException;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.dto.CreatePlacementRequest;
import com.adsplatform.placement.dto.PlacementResponse;
import com.adsplatform.placement.dto.UpdatePlacementRequest;

@ExtendWith(MockitoExtension.class)
class PlacementServiceTest {

    @Mock
    private PlacementRepository placementRepository;

    @InjectMocks
    private PlacementService placementService;

    private static CreatePlacementRequest createRequest(String name) {
        return new CreatePlacementRequest(
                name,
                PlacementType.PRE_ROLL,
                2,
                15,
                Set.of(DeviceType.TV, DeviceType.MOBILE),
                Set.of(CreativeType.VIDEO));
    }

    private static UpdatePlacementRequest updateRequest(String name) {
        return new UpdatePlacementRequest(
                name,
                PlacementType.MID_ROLL,
                3,
                30,
                Set.of(DeviceType.TV),
                Set.of(CreativeType.VIDEO));
    }

    private static Placement existingPlacement() {
        return Placement.create(
                "Pre-roll short",
                PlacementType.PRE_ROLL,
                2,
                15,
                Set.of(DeviceType.TV, DeviceType.MOBILE),
                Set.of(CreativeType.VIDEO));
    }

    @Nested
    class Create {

        @Test
        void savesAndReturnsInactivePlacement() {
            when(placementRepository.existsByNameIgnoreCase("Pre-roll short")).thenReturn(false);
            when(placementRepository.save(any(Placement.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PlacementResponse response =
                    placementService.createPlacement(createRequest("Pre-roll short"));

            assertThat(response.name()).isEqualTo("Pre-roll short");
            assertThat(response.active()).isFalse();
            assertThat(response.maxAdDurationSeconds()).isEqualTo(15);
            verify(placementRepository).save(any(Placement.class));
        }

        @Test
        void rejectsDuplicateNameWithoutSaving() {
            when(placementRepository.existsByNameIgnoreCase("Pre-roll short")).thenReturn(true);

            assertThatThrownBy(() ->
                    placementService.createPlacement(createRequest("Pre-roll short")))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Pre-roll short");

            verify(placementRepository, never()).save(any());
        }

        @Test
        void checksDuplicatesUsingTrimmedName() {
            when(placementRepository.existsByNameIgnoreCase("Pre-roll short")).thenReturn(true);

            assertThatThrownBy(() ->
                    placementService.createPlacement(createRequest("  Pre-roll short  ")))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        void blankNameIsRejectedByEntityWithoutDuplicateCheck() {
            assertThatThrownBy(() ->
                    placementService.createPlacement(createRequest("   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");

            verify(placementRepository, never()).existsByNameIgnoreCase(anyString());
            verify(placementRepository, never()).save(any());
        }

        @Test
        void entityRuleViolationIsNotSaved() {
            when(placementRepository.existsByNameIgnoreCase("Pre-roll short")).thenReturn(false);

            CreatePlacementRequest videoWithoutDuration = new CreatePlacementRequest(
                    "Pre-roll short",
                    PlacementType.PRE_ROLL,
                    2,
                    null,
                    Set.of(DeviceType.TV),
                    Set.of(CreativeType.VIDEO));

            assertThatThrownBy(() -> placementService.createPlacement(videoWithoutDuration))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duration");

            verify(placementRepository, never()).save(any());
        }
    }

    @Nested
    class Read {

        @Test
        void returnsPlacementById() {
            when(placementRepository.findById(1L)).thenReturn(Optional.of(existingPlacement()));

            PlacementResponse response = placementService.getPlacementById(1L);

            assertThat(response.name()).isEqualTo("Pre-roll short");
        }

        @Test
        void missingIdThrowsNotFound() {
            when(placementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> placementService.getPlacementById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        void listsReturnSortedDeviceAndCreativeTypes() {
            when(placementRepository.findAll()).thenReturn(List.of(existingPlacement()));

            List<PlacementResponse> responses = placementService.getAllPlacements();

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).supportedDevices())
                    .containsExactly(DeviceType.TV, DeviceType.MOBILE);
        }

        @Test
        void activePlacementsComeFromActiveQuery() {
            Placement active = existingPlacement();
            active.activate();
            when(placementRepository.findByActiveTrue()).thenReturn(List.of(active));

            List<PlacementResponse> responses = placementService.getActivePlacements();

            assertThat(responses).singleElement()
                    .satisfies(response -> assertThat(response.active()).isTrue());
        }
    }

    @Nested
    class ActiveStatus {

        @Test
        void activatesPlacement() {
            Placement placement = existingPlacement();
            when(placementRepository.findById(1L)).thenReturn(Optional.of(placement));

            PlacementResponse response = placementService.updatePlacementActiveStatus(1L, true);

            assertThat(response.active()).isTrue();
            assertThat(placement.isActive()).isTrue();
        }

        @Test
        void deactivatesPlacement() {
            Placement placement = existingPlacement();
            placement.activate();
            when(placementRepository.findById(1L)).thenReturn(Optional.of(placement));

            PlacementResponse response = placementService.updatePlacementActiveStatus(1L, false);

            assertThat(response.active()).isFalse();
        }

        @Test
        void missingIdThrowsNotFound() {
            when(placementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> placementService.updatePlacementActiveStatus(99L, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void appliesChangesWithoutCallingSave() {
            Placement placement = existingPlacement();
            when(placementRepository.findById(1L)).thenReturn(Optional.of(placement));
            when(placementRepository.existsByNameIgnoreCaseAndIdNot("Mid-roll standard", 1L))
                    .thenReturn(false);

            PlacementResponse response =
                    placementService.updatePlacement(1L, updateRequest("Mid-roll standard"));

            assertThat(response.name()).isEqualTo("Mid-roll standard");
            assertThat(response.type()).isEqualTo(PlacementType.MID_ROLL);
            assertThat(response.maxAdDurationSeconds()).isEqualTo(30);

            // The loaded entity is managed, so changes are written at commit.
            verify(placementRepository, never()).save(any());
        }

        @Test
        void keepingOwnNameIsNotADuplicate() {
            Placement placement = existingPlacement();
            when(placementRepository.findById(1L)).thenReturn(Optional.of(placement));
            when(placementRepository.existsByNameIgnoreCaseAndIdNot("Pre-roll short", 1L))
                    .thenReturn(false);

            PlacementResponse response =
                    placementService.updatePlacement(1L, updateRequest("Pre-roll short"));

            assertThat(response.name()).isEqualTo("Pre-roll short");
        }

        @Test
        void nameTakenByAnotherPlacementIsRejected() {
            Placement placement = existingPlacement();
            when(placementRepository.findById(1L)).thenReturn(Optional.of(placement));
            when(placementRepository.existsByNameIgnoreCaseAndIdNot("Pause screen", 1L))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    placementService.updatePlacement(1L, updateRequest("Pause screen")))
                    .isInstanceOf(DuplicateResourceException.class);

            assertThat(placement.getName()).isEqualTo("Pre-roll short");
        }

        @Test
        void missingIdThrowsNotFound() {
            when(placementRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    placementService.updatePlacement(99L, updateRequest("Anything")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}