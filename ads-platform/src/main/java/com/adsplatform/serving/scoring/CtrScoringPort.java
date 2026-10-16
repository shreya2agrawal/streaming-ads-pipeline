package com.adsplatform.serving.scoring;

import java.util.List;

import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Predicts how likely the viewer is to click each candidate.
 *
 * Scores a whole batch in one call: a remote model service then
 * needs one round trip per ad request, not one per candidate.
 *
 * Returns one probability (0 to 1) per candidate, in the same order.
 * Predicts only; ranking and pricing are decided elsewhere.
 */
public interface CtrScoringPort {
    
    List<Double> predictCtr(
        List<CandidateAd> candidates,
        AdRequestContext context
    );
}
