package com.adsplatform.serving.conversion;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "ad_conversions")
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdConversion {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="click_token", nullable = false)
    private String clickToken;

    @Column(name="campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "booking_id", nullable = false)
    private String bookingId;

    @Column(name = "value", nullable = false)
    private BigDecimal value;

    @Column(name = "currency", nullable = false, columnDefinition = "bpchar(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConversionStatus status;

    @Column(name = "converted_at", nullable = false)
    private Instant convertedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

}
