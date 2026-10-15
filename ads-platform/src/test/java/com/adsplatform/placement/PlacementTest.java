package com.adsplatform.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.adsplatform.creative.CreativeType;

class PlacementTest {

    private static Placement videoPlacement() {
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
        void createsValidVideoPlacement() {
            Placement placement = videoPlacement();

            assertThat(placement.getName()).isEqualTo("Pre-roll short");
            assertThat(placement.getType()).isEqualTo(PlacementType.PRE_ROLL);
            assertThat(placement.getMaxAds()).isEqualTo(2);
            assertThat(placement.getMaxAdDurationSeconds()).isEqualTo(15);
            assertThat(placement.getSupportedDevices())
                    .containsExactlyInAnyOrder(DeviceType.TV, DeviceType.MOBILE);
            assertThat(placement.getSupportedCreativeTypes())
                    .containsExactly(CreativeType.VIDEO);
        }

        @Test
        void startsInactiveSoItCannotReceiveTrafficUntilTurnedOn() {
            assertThat(videoPlacement().isActive()).isFalse();
        }

        @Test
        void trimsName() {
            Placement placement = Placement.create(
                    "  Pause screen  ",
                    PlacementType.PAUSE_SCREEN,
                    1,
                    null,
                    Set.of(DeviceType.TV),
                    Set.of(CreativeType.IMAGE));

            assertThat(placement.getName()).isEqualTo("Pause screen");
        }

        @Test
        void allowsNonVideoPlacementWithoutDuration() {
            Placement placement = Placement.create(
                    "Pause screen",
                    PlacementType.PAUSE_SCREEN,
                    1,
                    null,
                    Set.of(DeviceType.TV),
                    Set.of(CreativeType.IMAGE, CreativeType.NATIVE));

            assertThat(placement.getMaxAdDurationSeconds()).isNull();
        }

        @Test
        void allowsMixedPlacementThatAcceptsVideoWithDuration() {
            Placement placement = Placement.create(
                    "Home hero",
                    PlacementType.HOME_HERO,
                    1,
                    20,
                    Set.of(DeviceType.TV, DeviceType.WEB),
                    Set.of(CreativeType.VIDEO, CreativeType.IMAGE));

            assertThat(placement.getMaxAdDurationSeconds()).isEqualTo(20);
        }
    }

    @Nested
    class Rules {

        @Test
        void rejectsBlankName() {
            assertThatThrownBy(() -> Placement.create(
                    "   ", PlacementType.PRE_ROLL, 1, 15,
                    Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        void rejectsNullName() {
            assertThatThrownBy(() -> Placement.create(
                    null, PlacementType.PRE_ROLL, 1, 15,
                    Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejectsMissingType() {
            assertThatThrownBy(() -> Placement.create(
                    "Pre-roll", null, 1, 15,
                    Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("type");
        }

        @Test
        void rejectsMaxAdsBelowOne() {
            assertThatThrownBy(() -> Placement.create(
                    "Pre-roll", PlacementType.PRE_ROLL, 0, 15,
                    Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Max ads");
        }

        @Test
        void rejectsEmptyDevices() {
            assertThatThrownBy(() -> Placement.create(
                    "Pre-roll", PlacementType.PRE_ROLL, 1, 15,
                    Set.of(), Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("device");
        }

        @Test
        void rejectsNullDevices() {
            assertThatThrownBy(() -> Placement.create(
                    "Pre-roll", PlacementType.PRE_ROLL, 1, 15,
                    null, Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("device");
        }

        @Test
        void rejectsEmptyCreativeTypes() {
            assertThatThrownBy(() -> Placement.create(
                    "Pre-roll", PlacementType.PRE_ROLL, 1, 15,
                    Set.of(DeviceType.TV), Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("creative type");
        }

        @Test
        void rejectsVideoPlacementWithoutDuration() {
            assertThatThrownBy(() -> Placement.create(
                    "Pre-roll", PlacementType.PRE_ROLL, 1, null,
                    Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duration");
        }

        @Test
        void rejectsNonVideoPlacementWithDuration() {
            assertThatThrownBy(() -> Placement.create(
                    "Pause screen", PlacementType.PAUSE_SCREEN, 1, 15,
                    Set.of(DeviceType.TV), Set.of(CreativeType.IMAGE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duration");
        }

        @Test
        void rejectsNonPositiveDuration() {
            assertThatThrownBy(() -> Placement.create(
                    "Pre-roll", PlacementType.PRE_ROLL, 1, 0,
                    Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duration");
        }
    }

    @Nested
    class Update {

        @Test
        void appliesAllFields() {
            Placement placement = videoPlacement();

            placement.update(
                    "Mid-roll standard",
                    PlacementType.MID_ROLL,
                    3,
                    30,
                    Set.of(DeviceType.TV),
                    Set.of(CreativeType.VIDEO));

            assertThat(placement.getName()).isEqualTo("Mid-roll standard");
            assertThat(placement.getType()).isEqualTo(PlacementType.MID_ROLL);
            assertThat(placement.getMaxAds()).isEqualTo(3);
            assertThat(placement.getMaxAdDurationSeconds()).isEqualTo(30);
            assertThat(placement.getSupportedDevices()).containsExactly(DeviceType.TV);
        }

        @Test
        void keepsActiveStateBecauseUpdateIsNotActivation() {
            Placement placement = videoPlacement();
            placement.activate();

            placement.update(
                    "Renamed", PlacementType.PRE_ROLL, 2, 15,
                    Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO));

            assertThat(placement.isActive()).isTrue();
        }

        @Test
        void invalidUpdateLeavesPlacementUnchanged() {
            Placement placement = videoPlacement();

            assertThatThrownBy(() -> placement.update(
                    "Renamed", PlacementType.PRE_ROLL, 2, null,
                    Set.of(DeviceType.TV), Set.of(CreativeType.VIDEO)))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(placement.getName()).isEqualTo("Pre-roll short");
            assertThat(placement.getMaxAdDurationSeconds()).isEqualTo(15);
        }

        @Test
        void rejectsNullSetsInsteadOfCrashing() {
            Placement placement = videoPlacement();

            assertThatThrownBy(() -> placement.update(
                    "Renamed", PlacementType.PRE_ROLL, 2, 15,
                    null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Activation {

        @Test
        void activateAndDeactivate() {
            Placement placement = videoPlacement();

            placement.activate();
            assertThat(placement.isActive()).isTrue();

            placement.deactivate();
            assertThat(placement.isActive()).isFalse();
        }
    }

    @Nested
    class Encapsulation {

        @Test
        void callerChangingTheirSetAfterCreateDoesNotChangePlacement() {
            Set<DeviceType> devices = new HashSet<>(Set.of(DeviceType.TV));

            Placement placement = Placement.create(
                    "Pre-roll", PlacementType.PRE_ROLL, 1, 15,
                    devices, Set.of(CreativeType.VIDEO));

            devices.add(DeviceType.WEB);

            assertThat(placement.getSupportedDevices()).containsExactly(DeviceType.TV);
        }

        @Test
        void returnedSetsAreReadOnly() {
            Placement placement = videoPlacement();

            assertThatThrownBy(() -> placement.getSupportedDevices().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> placement.getSupportedCreativeTypes().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Support {

        @Test
        void supportsListedDeviceAndType() {
            Placement placement = videoPlacement();

            assertThat(placement.supportsDevice(DeviceType.TV)).isTrue();
            assertThat(placement.supportsDevice(DeviceType.WEB)).isFalse();
            assertThat(placement.supportsCreativeType(CreativeType.VIDEO)).isTrue();
            assertThat(placement.supportsCreativeType(CreativeType.IMAGE)).isFalse();
        }

        @Test
        void nullIsNeverSupported() {
            Placement placement = videoPlacement();

            assertThat(placement.supportsDevice(null)).isFalse();
            assertThat(placement.supportsCreativeType(null)).isFalse();
        }
    }
}