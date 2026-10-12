package com.adsplatform.safety.dto;

import com.adsplatform.safety.SafetyReviewStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SafetyReviewDecisionRequest(

        @NotNull
        SafetyReviewStatus status,

        @NotBlank
        String reviewer,

        String reason

) {
}