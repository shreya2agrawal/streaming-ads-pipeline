package com.adsplatform.serving.eligibility;

public enum IneligibilityReason {

    // Campaign
    CAMPAIGN_NOT_ACTIVE,
    FLIGHT_NOT_STARTED,
    FLIGHT_ENDED,

    // Placement
    PLACEMENT_MISMATCH,
    PLACEMENT_NOT_ACTIVE,
    DEVICE_NOT_SUPPORTED,
    CREATIVE_TYPE_NOT_SUPPORTED,
    CREATIVE_TOO_LONG,

    // Campaign targeting
    PLACEMENT_NOT_TARGETED,
    DEVICE_NOT_TARGETED,
    SHOW_NOT_TARGETED,
    AUDIENCE_NOT_MATCHED,

    // Creative
    CREATIVE_NOT_ACTIVE,
    CREATIVE_NOT_APPROVED
}