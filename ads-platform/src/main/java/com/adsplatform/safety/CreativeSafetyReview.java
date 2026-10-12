package com.adsplatform.safety;

import java.time.LocalDateTime;

import com.adsplatform.creative.Creative;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "creative_safety_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreativeSafetyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creative_id", nullable = false)
    private Creative creative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SafetyReviewStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_source", nullable = false)
    private SafetyReviewSource reviewSource;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private Double score;

    private String modelVersion;

    private String policyVersion;

    private String reviewer;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = SafetyReviewStatus.PENDING;
        }
    }
}