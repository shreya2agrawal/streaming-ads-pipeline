package com.adsplatform.campaign;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
