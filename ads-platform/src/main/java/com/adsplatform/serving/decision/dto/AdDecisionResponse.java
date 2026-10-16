package com.adsplatform.serving.decision.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.adsplatform.creative.CreativeType;
import com.adsplatform.serving.decision.Exclusion;
import com.fasterxml.jackson.annotation.JsonInclude;

/*
 * What the player receives.
 *
 * ads carries only what a player needs to show an ad and track a
 * click. The advertiser's destination is never included: viewers
 * must go through clickUrl, so every click is recorded.
 *
 * explanation (prices, scores, exclusions) is commercially
 * sensitive and is only included in debug mode.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdDecisionResponse(
        UUID decisionId,
        List<ServedAd> ads,
        String noFillReason,
        Explanation explanation) {

    public record ServedAd(
            int slot,
            Long campaignId,
            Long creativeId,
            CreativeType creativeType,
            String mediaUrl,
            Integer durationSeconds,
            String clickUrl) {
    }

    public record Explanation(
            int candidateCount,
            List<ScoredAd> winners,
            List<Exclusion> excluded) {
    }

    public record ScoredAd(
            int slot,
            Long campaignId,
            Long creativeId,
            BigDecimal priceCpc,
            double predictedCtr,
            double score) {
    }
}