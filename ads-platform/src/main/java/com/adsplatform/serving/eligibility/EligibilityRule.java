package com.adsplatform.serving.eligibility;

import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Extension point for serving eligibility.
 *
 * Every rule checks one responsibility only.
 *
 * Examples:
 * - CampaignStatusRule
 * - FlightDateRule
 * - PlacementCompatibilityRule
 * - CreativeStatusRule
 * - CreativeSafetyRule
 *
 * New rules can be added without changing
 * the EligibilityEngine.
 */
public interface EligibilityRule {

    EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context
    );

}
