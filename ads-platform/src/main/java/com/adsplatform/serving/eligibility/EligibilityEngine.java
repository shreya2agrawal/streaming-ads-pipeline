package com.adsplatform.serving.eligibility;

import java.util.List;

import org.springframework.stereotype.Service;

import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Runs all registered eligibility rules for a candidate.
 */
@Service
public class EligibilityEngine {

    private final List<EligibilityRule> eligibilityRules;

    public EligibilityEngine(
            List<EligibilityRule> eligibilityRules) {

        this.eligibilityRules =
                List.copyOf(eligibilityRules);
    }

    public EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context) {

        if (candidate == null) {
            throw new IllegalArgumentException(
                    "Candidate Ad is required"
            );
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "Ad Request Context is required"
            );
        }

        for (EligibilityRule rule : eligibilityRules) {

            EligibilityResult result =
                    rule.evaluate(
                            candidate,
                            context
                    );

            if (!result.eligible()) {
                return result;
            }
        }

        return EligibilityResult.allowed();
    }
}