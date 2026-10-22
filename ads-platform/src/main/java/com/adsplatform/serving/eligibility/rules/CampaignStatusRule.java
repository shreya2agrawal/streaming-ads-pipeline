package com.adsplatform.serving.eligibility.rules;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.adsplatform.campaign.Campaign.CampaignStatus;
import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.eligibility.EligibilityRule;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

@Component 
@Order(10)
public class CampaignStatusRule implements EligibilityRule {

    @Override
    public EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context) {

        CampaignStatus campaignStatus = candidate.campaign().status();

        if (campaignStatus != CampaignStatus.ACTIVE) {
            return EligibilityResult.ineligible(
                    IneligibilityReason.CAMPAIGN_NOT_ACTIVE,
                    "Campaign is not active");
        }

        return EligibilityResult.allowed();
    }

}
