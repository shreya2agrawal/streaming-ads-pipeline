package com.adsplatform.serving.decision;

import java.math.BigDecimal;

import com.adsplatform.serving.model.CandidateAd;

/*
 * An eligible candidate with its predicted click-through rate
 * and ranking score.
 *
 * score = bid x pCTR: the expected earning per showing. It only
 * orders candidates and is never charged, so a double is fine.
 * The price charged on a click is the bid, kept as BigDecimal.
 */
public record RankedAd(
    CandidateAd candidate,
    double predictedCtr,
    double score
) {
   
    public Long campaignId() {
        return candidate.campaign().id();
    }

    public Long creativeId() {
        return candidate.creative().id();
    }

    public BigDecimal priceCpc() {
        return candidate.campaign().bidCpc();
    }

}
