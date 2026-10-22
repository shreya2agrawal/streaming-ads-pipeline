package com.adsplatform.serving.eligibility.rules;

import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.adsplatform.placement.DeviceType;
import com.adsplatform.serving.eligibility.CreativeFit;
import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.eligibility.EligibilityRule;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Checks whether the current ad opportunity is compatible
 * with the Campaign, Placement and Creative.
 */
@Component
@Order(30)
public class PlacementCompatibilityRule
        implements EligibilityRule {

    @Override
    public EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context) {

        CandidateAd.CampaignSnapshot campaign =
                candidate.campaign();

        CandidateAd.CreativeSnapshot creative =
                candidate.creative();

        CandidateAd.PlacementSnapshot placement =
                candidate.placement();

        Long requestPlacementId =
                context.placementId();

        DeviceType requestDevice =
                context.deviceType();

        if (!placement.id().equals(requestPlacementId)) {
            return EligibilityResult.ineligible(
                    IneligibilityReason.PLACEMENT_MISMATCH,
                    "Candidate placement does not match requested placement"
            );
        }

        if (!placement.active()) {
            return EligibilityResult.ineligible(
                    IneligibilityReason.PLACEMENT_NOT_ACTIVE,
                    "Placement is not active"
            );
        }

        /*
         * Empty Campaign placement targeting means
         * no explicit placement restriction.
         */
        Set<Long> targetedPlacements =
                campaign.targetPlacementIds();

        if (!targetedPlacements.isEmpty()
                && !targetedPlacements.contains(requestPlacementId)) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.PLACEMENT_NOT_TARGETED,
                    "Campaign does not target requested placement"
            );
        }

        /*
         * Make sure the Placement itself supports
         * the device generating this opportunity.
         */
        if (!placement.supportedDevices()
                .contains(requestDevice)) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.DEVICE_NOT_SUPPORTED,
                    "Placement does not support requested device"
            );
        }

        /*
         * Campaign may additionally restrict which devices
         * it wants to serve on.
         */
        Set<DeviceType> targetedDevices =
                campaign.targetDeviceTypes();

        if (!targetedDevices.isEmpty()
                && !targetedDevices.contains(requestDevice)) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.DEVICE_NOT_TARGETED,
                    "Campaign does not target requested device"
            );
        }

        /*
         * Actual creative must be supported by this Placement.
         */
        return CreativeFit.check(
                placement.supportedCreativeTypes(),
                placement.maxAdDurationSeconds(),
                creative.creativeType(),
                creative.durationSeconds()
        );

    }
}