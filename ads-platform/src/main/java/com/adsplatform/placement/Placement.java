package com.adsplatform.placement;

import java.util.HashSet;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "placements")
@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
public class Placement {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlacementType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    private Integer maxAds;
    private Integer durationSeconds;

    @ElementCollection 
    @CollectionTable(name = "placement_supported_devices",
        joinColumns = @JoinColumn(name = "placement_id")
    )
    @Enumerated(EnumType.STRING)
    @Column (name = "device_type", nullable = false)
    @Builder.Default
    private Set<DeviceType> supportedDevices = new HashSet<>();

    @ElementCollection 
    @CollectionTable(
        name = "placement_supported_creative_types",
        joinColumns = @JoinColumn(name = "placement_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "creative_type", nullable = false)
    @Builder.Default
    private Set<CreativeType> supportedCreativeTypes = new HashSet<>();

    public boolean supportsDevice(DeviceType deviceType){
        return deviceType != null && supportedDevices.contains(deviceType);
    }

    public boolean supportsCreativeType(CreativeType creativeType){
        return creativeType != null && supportedCreativeTypes.contains(creativeType);
    }
}
