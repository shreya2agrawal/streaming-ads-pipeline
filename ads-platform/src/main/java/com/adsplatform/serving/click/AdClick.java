package com.adsplatform.serving.click;

import java.math.BigDecimal;
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
 * A counted click: the billing record.
 *
 * Rows are written only by AdClickRepository.insertIfAbsent,
 * so there is no public way to construct one in Java.
 */
@Entity
@Table(name = "ad_clicks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "click_token", nullable = false, unique = true)
    private String clickToken;

    @Column(name = "decision_id", nullable = false)
    private UUID decisionId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "creative_id", nullable = false)
    private Long creativeId;

    @Column(name = "charged_amount", nullable = false)
    private BigDecimal chargedAmount;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;
}