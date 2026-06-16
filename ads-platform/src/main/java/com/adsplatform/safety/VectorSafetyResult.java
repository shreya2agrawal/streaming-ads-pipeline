package com.adsplatform.safety;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VectorSafetyResult {
    private SafetyDecision decision;
    private double score;
    private String reason;
    private String topMatchTitle;

    public enum SafetyDecision{SAFE, REVIEW, BLOCK}
}
