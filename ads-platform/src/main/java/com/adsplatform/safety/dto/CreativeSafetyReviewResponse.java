package com.adsplatform.safety.dto;

import java.time.LocalDateTime;

import com.adsplatform.safety.SafetyReviewSource;
import com.adsplatform.safety.SafetyReviewStatus;

public record CreativeSafetyReviewResponse(

        Long id,
        Long creativeId,
        SafetyReviewStatus status,
        SafetyReviewSource reviewSource,
        String reason,
        Double score,
        String modelVersion,
        String policyVersion,
        String reviewer,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt

) {
}