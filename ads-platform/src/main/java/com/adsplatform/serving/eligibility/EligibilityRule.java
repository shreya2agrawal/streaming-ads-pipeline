package com.adsplatform.serving.eligibility;

import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Extension point for serving eligibility.
 * Every rule checks one responsibility only.
 */
public interface EligibilityRule {

    EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context
    );

}
