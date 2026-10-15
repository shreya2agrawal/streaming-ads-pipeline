package com.adsplatform.serving.eligibility.rules;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.eligibility.EligibilityRule;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Only ACTIVE creatives may participate
 * in real-time ad serving.
 */
@Component
@Order(40)
public class CreativeStatusRule implements EligibilityRule {

    @Override
    public EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context) {

        CreativeStatus creativeStatus =
                candidate.creative().creativeStatus();

        if (creativeStatus != CreativeStatus.ACTIVE) {
            return EligibilityResult.ineligible(
                    IneligibilityReason.CREATIVE_NOT_ACTIVE,
                    "Creative is not active"
            );
        }

        return EligibilityResult.allowed();
    }
}