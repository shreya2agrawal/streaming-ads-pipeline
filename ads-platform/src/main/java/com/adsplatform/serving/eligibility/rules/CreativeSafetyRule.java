package com.adsplatform.serving.eligibility.rules;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.eligibility.EligibilityRule;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Only creatives that completed the safety workflow
 * and were APPROVED may participate in serving.
 */
@Component
@Order(50)
public class CreativeSafetyRule implements EligibilityRule {

    @Override
    public EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context) {

        CreativeSafetyStatus safetyStatus =
                candidate.creative()
                        .creativeSafetyStatus();

        if (safetyStatus != CreativeSafetyStatus.APPROVED) {
            return EligibilityResult.ineligible(
                    IneligibilityReason.CREATIVE_NOT_APPROVED,
                    "Creative is not safety approved"
            );
        }

        return EligibilityResult.allowed();
    }
}