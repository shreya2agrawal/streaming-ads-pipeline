package com.adsplatform.campaign;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.adsplatform.placement.DeviceType;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "campaigns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String advertiser;

    @Column(nullable = false)
    private String agency;

    @Column(nullable = false)
    private BigDecimal budget;

    @Column(nullable = false)
    private LocalDate flightStart;

    @Column(nullable = false)
    private LocalDate flightEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    private Double pacingPercent;
    private Long impressionsDelivered;
    private Long impressionsGoal;

    /*
     * Maximum the advertiser pays per click.
     * Null means no bid yet, so the campaign does not compete.
     */
    @Column(name = "bid_cpc")
    private BigDecimal bidCpc;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal spent = BigDecimal.ZERO;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable (name = "campaign_target_shows", 
    joinColumns = @JoinColumn(name = "campaign_id"))
    @Column (name = "show_id")
    @Builder.Default
    private Set<String> targetShowIds = new HashSet<>();

    @ElementCollection 
    @CollectionTable (name = "campaign_targeting_segments", 
    joinColumns = @JoinColumn(name = "campaign_id"))
    @Column (name = "segment")
    @Builder.Default
    private Set<String> targetingSegments = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "campaign_target_placements", 
    joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "placement_id")
    @Builder.Default
    private Set<Long> targetPlacementIds = new HashSet<>();

    @ElementCollection 
    @CollectionTable (name = "campaign_target_devices", 
    joinColumns = @JoinColumn(name = "campaign_id"))
    @Column (name = "device_type")
    @Enumerated (EnumType.STRING)
    @Builder.Default
    private Set<DeviceType> targetDeviceTypes = new HashSet<>();

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

    public enum CampaignStatus{
        DRAFT, ACTIVE, PAUSED, COMPLETED, AT_RISK, PACING_LOW
    }
}
