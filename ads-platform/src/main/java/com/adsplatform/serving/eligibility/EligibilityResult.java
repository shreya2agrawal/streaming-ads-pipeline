package com.adsplatform.serving.eligibility;

public record EligibilityResult(
        boolean eligible,
        IneligibilityReason reasonCode,
        String reason
) {

    public static EligibilityResult allowed() {
        return new EligibilityResult(
                true,
                null,
                null
        );
    }

    public static EligibilityResult ineligible(
            IneligibilityReason reasonCode,
            String reason) {

        if (reasonCode == null) {
            throw new IllegalArgumentException(
                    "Ineligibility reason code is required"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Ineligibility reason is required"
            );
        }

        return new EligibilityResult(
                false,
                reasonCode,
                reason
        );
    }
}