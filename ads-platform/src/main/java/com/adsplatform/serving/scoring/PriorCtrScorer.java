package com.adsplatform.serving.scoring;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Starting estimate used until real click data exists:
 * every candidate gets the same prior click-through rate.
 *
 * With equal scores, ranking orders by bid. A model trained on the
 * platform's own impressions and clicks replaces this behind the
 * same port.
 */
@Component 
public class PriorCtrScorer implements CtrScoringPort {
    
    static final String DEFAULT_PRIOR = "0.02";

    private final double prior;

    public PriorCtrScorer(
        @Value("${serving.ctr.prior:" + DEFAULT_PRIOR + "}") double prior
    ){
        if(prior <= 0 || prior >= 1){
            throw new IllegalArgumentException("CTR prior must be between 0 and 1, exclusive");
        }
        this.prior = prior;
    }

    @Override
    public List<Double> predictCtr(
        List<CandidateAd> candidates, 
        AdRequestContext context) {
        return Collections.nCopies(candidates.size(), prior);
    }

}
