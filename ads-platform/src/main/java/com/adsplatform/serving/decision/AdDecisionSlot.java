package com.adsplatform.serving.decision;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * One filled slot in a decision: which ad, at what price,
 * and the click token that identifies it.
 *
 * priceCpc is the price at auction time. A click is charged this,
 * even if the advertiser changes the bid afterwards.
 */
@Entity
@Table(name = "ad_decision_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdDecisionSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_id", nullable = false)
    private AdDecision decision;

    @Column(name = "slot_position", nullable = false)
    private int slotPosition;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "creative_id", nullable = false)
    private Long creativeId;

    @Column(name = "price_cpc", nullable = false)
    private BigDecimal priceCpc;

    @Column(name = "predicted_ctr", nullable = false)
    private double predictedCtr;

    @Column(name = "ranking_score", nullable = false)
    private double rankingScore;

    @Column(name = "click_token", nullable = false, unique = true)
    private String clickToken;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    AdDecisionSlot(
            AdDecision decision,
            int slotPosition,
            RankedAd ad,
            String clickToken,
            Instant createdAt) {

        this.decision = decision;
        this.slotPosition = slotPosition;
        this.campaignId = ad.campaignId();
        this.creativeId = ad.creativeId();
        this.priceCpc = ad.priceCpc();
        this.predictedCtr = ad.predictedCtr();
        this.rankingScore = ad.score();
        this.clickToken = clickToken;
        this.createdAt = createdAt;
    }
}