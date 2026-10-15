package com.adsplatform.serving.eligibility;

import java.util.Set;

import com.adsplatform.creative.CreativeType;

public final class CreativeFit {

    private CreativeFit() {
    }

    public static EligibilityResult check(
            Set<CreativeType> supportedCreativeTypes,
            Integer maxAdDurationSeconds,
            CreativeType creativeType,
            Integer creativeDurationSeconds) {

        if (supportedCreativeTypes == null
                || !supportedCreativeTypes.contains(creativeType)) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.CREATIVE_TYPE_NOT_SUPPORTED,
                    "Creative type is not supported by placement"
            );
        }

        /*
         * Length only applies to video. Images and native ads
         * have no running time to compare.
         */
        if (creativeType != CreativeType.VIDEO) {
            return EligibilityResult.allowed();
        }

        if (creativeDurationSeconds == null) {
            throw new IllegalArgumentException(
                    "Video creative duration is required"
            );
        }

        /*
         * No limit configured means any length fits.
         */
        if (maxAdDurationSeconds != null
                && creativeDurationSeconds > maxAdDurationSeconds) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.CREATIVE_TOO_LONG,
                    "Creative is " + creativeDurationSeconds
                            + "s but placement allows up to "
                            + maxAdDurationSeconds + "s"
            );
        }

        return EligibilityResult.allowed();
    }
}