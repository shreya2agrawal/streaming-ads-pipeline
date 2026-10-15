package com.adsplatform.placement;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.adsplatform.creative.CreativeType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "placements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Placement {

    static final int MAX_NAME_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlacementType type;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Integer maxAds;

    /*
     * Maximum length of each individual ad, not of the whole break.
     * Present only when the placement accepts VIDEO.
     */
    @Column(name = "max_ad_duration_seconds")
    private Integer maxAdDurationSeconds;

    @ElementCollection
    @CollectionTable(name = "placement_supported_devices", joinColumns = @JoinColumn(name = "placement_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private Set<DeviceType> supportedDevices = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "placement_supported_creative_types", joinColumns = @JoinColumn(name = "placement_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "creative_type", nullable = false)
    private Set<CreativeType> supportedCreativeTypes = new HashSet<>();

    /*
     * New placements start inactive so they cannot receive traffic
     * until someone deliberately turns them on.
     */
    public static Placement create(
            String name,
            PlacementType type,
            Integer maxAds,
            Integer maxAdDurationSeconds,
            Set<DeviceType> supportedDevices,
            Set<CreativeType> supportedCreativeTypes) {

        Placement placement = new Placement();
        placement.apply(
                name,
                type,
                maxAds,
                maxAdDurationSeconds,
                supportedDevices,
                supportedCreativeTypes);
        placement.active = false;
        return placement;
    }

    /*
     * Replaces the configuration. Does not change active state:
     * editing a placement is not the same decision as turning it on.
     */
    public void update(
            String name,
            PlacementType type,
            Integer maxAds,
            Integer maxAdDurationSeconds,
            Set<DeviceType> supportedDevices,
            Set<CreativeType> supportedCreativeTypes) {

        apply(
                name,
                type,
                maxAds,
                maxAdDurationSeconds,
                supportedDevices,
                supportedCreativeTypes);
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public boolean supportsDevice(DeviceType deviceType) {
        return deviceType != null && supportedDevices.contains(deviceType);
    }

    public boolean supportsCreativeType(CreativeType creativeType) {
        return creativeType != null && supportedCreativeTypes.contains(creativeType);
    }

    public Set<DeviceType> getSupportedDevices() {
        return Collections.unmodifiableSet(supportedDevices);
    }

    public Set<CreativeType> getSupportedCreativeTypes() {
        return Collections.unmodifiableSet(supportedCreativeTypes);
    }

    /*
     * Validate everything first, then assign. A failed update
     * must leave the placement exactly as it was.
     */
    private void apply(
            String name,
            PlacementType type,
            Integer maxAds,
            Integer maxAdDurationSeconds,
            Set<DeviceType> devices,
            Set<CreativeType> creativeTypes) {

        String trimmedName = validateName(name);

        if (type == null) {
            throw new IllegalArgumentException(
                    "Placement type is required");
        }

        if (maxAds == null || maxAds <= 0) {
            throw new IllegalArgumentException(
                    "Max ads must be greater than zero");
        }

        Set<DeviceType> deviceCopy = copyNonEmpty(devices, "Placement must support at least one device");
        Set<CreativeType> creativeCopy = copyNonEmpty(creativeTypes,
                "Placement must support at least one creative type");

        validateDuration(maxAdDurationSeconds, creativeCopy);

        this.name = trimmedName;
        this.type = type;
        this.maxAds = maxAds;
        this.maxAdDurationSeconds = maxAdDurationSeconds;

        // Mutate the managed collections instead of replacing them,
        // so Hibernate tracks the change on the existing collection.
        this.supportedDevices.clear();
        this.supportedDevices.addAll(deviceCopy);
        this.supportedCreativeTypes.clear();
        this.supportedCreativeTypes.addAll(creativeCopy);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Placement name is required");
        }

        String trimmed = name.trim();

        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Placement name must be at most "
                            + MAX_NAME_LENGTH
                            + " characters");
        }
        return trimmed;
    }

    /*
     * Duration only means something for video: required when the
     * placement accepts VIDEO, and absent when it does not.
     */
    private static void validateDuration(
            Integer maxAdDurationSeconds,
            Set<CreativeType> creativeTypes) {
        boolean acceptsVideo = creativeTypes.contains(CreativeType.VIDEO);
        if (acceptsVideo && maxAdDurationSeconds == null) {
            throw new IllegalArgumentException(
                    "Placements that accept video require a max ad duration");
        }
        if (!acceptsVideo && maxAdDurationSeconds != null) {
            throw new IllegalArgumentException(
                    "Placements without video must not have a max ad duration");
        }
        if (maxAdDurationSeconds != null && maxAdDurationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Max ad duration must be greater than zero");
        }
    }

    private static <T> Set<T> copyNonEmpty(Set<T> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(message);
        }
        return new HashSet<>(values);
    }
}
