package com.adsplatform.serving.decision;

import com.adsplatform.serving.eligibility.IneligibilityReason;

public record Exclusion(
    Long campaignId,
    Long creativeId,
    IneligibilityReason reasonCode,
    String reason
) {
    
}
