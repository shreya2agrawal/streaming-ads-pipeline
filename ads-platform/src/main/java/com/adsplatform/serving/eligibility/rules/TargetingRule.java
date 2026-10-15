package com.adsplatform.serving.eligibility.rules;

import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.eligibility.EligibilityRule;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Applies the Campaign targeting information currently
 * available in the platform.
 *
 * targetShowIds:
 * An explicit allowlist, e.g. for a sponsorship deal or an
 * internal override. Empty means no show restriction.
 *
 * targetingSegments:
 * Campaign is eligible when the current viewer matches
 * at least one requested audience segment.
 *
 * Platform-generated targeting/recommendation logic can
 * later be added independently without changing the engine.
 */
@Component
@Order(60)
public class TargetingRule implements EligibilityRule {

    @Override
    public EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context) {

        Set<String> targetShowIds =
                candidate.campaign()
                        .targetShowIds();

        Set<String> campaignSegments =
                candidate.campaign()
                        .targetingSegments();

        /*
         * Empty show targeting means there is no manual
         * show restriction.
         *
         * Non-empty values act as an explicit allowlist.
         */
        if (!targetShowIds.isEmpty()
                && !targetShowIds.contains(
                        context.showId()
                )) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.SHOW_NOT_TARGETED,
                    "Campaign is not eligible for this show"
            );
        }

        /*
         * No audience targeting means the Campaign
         * does not require a specific audience segment.
         */
        if (campaignSegments.isEmpty()) {
            return EligibilityResult.allowed();
        }

        Set<String> viewerSegments =
                context.audienceSegments();

        if (viewerSegments == null
                || viewerSegments.isEmpty()) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.AUDIENCE_NOT_MATCHED,
                    "Viewer does not match campaign audience targeting"
            );
        }

        boolean audienceMatch =
                campaignSegments
                        .stream()
                        .anyMatch(
                                viewerSegments::contains
                        );

        if (!audienceMatch) {
            return EligibilityResult.ineligible(
                    IneligibilityReason.AUDIENCE_NOT_MATCHED,
                    "Viewer does not match campaign audience targeting"
            );
        }

        return EligibilityResult.allowed();
    }
}