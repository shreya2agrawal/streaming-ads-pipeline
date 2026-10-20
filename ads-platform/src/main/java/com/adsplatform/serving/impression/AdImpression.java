package com.adsplatform.serving.impression;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * An ad that was actually shown, as reported by the player.
 *
 * Rows are written only by AdImpressionRepository.insertIfAbsent,
 * so there is no public way to construct one in Java.
 */

@Entity 
@Table(name = "ad_impressions")
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED) 
public class AdImpression {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_token", nullable = false, unique = true)
    private String slotToken;

    @Column(name = "decision_id", nullable = false)
    private UUID decisionId;
    
    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "creative_id", nullable = false)
    private Long creativeId;

    @Column(name = "impressed_at", nullable = false)
    private Instant impressedAt;
}
