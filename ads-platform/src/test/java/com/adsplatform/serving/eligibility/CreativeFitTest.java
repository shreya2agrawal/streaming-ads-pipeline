package com.adsplatform.serving.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.adsplatform.campaign.Campaign.CampaignStatus;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.serving.eligibility.rules.PlacementCompatibilityRule;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

class CreativeFitTest {

    private static final Set<CreativeType> VIDEO_ONLY = Set.of(CreativeType.VIDEO);
    private static final Set<CreativeType> VIDEO_AND_IMAGE =
            Set.of(CreativeType.VIDEO, CreativeType.IMAGE);

    @Nested
    class Type {

        @Test
        void unsupportedTypeDoesNotFit() {
            EligibilityResult result =
                    CreativeFit.check(VIDEO_ONLY, 30, CreativeType.IMAGE, null);

            assertThat(result.eligible()).isFalse();
            assertThat(result.reasonCode())
                    .isEqualTo(IneligibilityReason.CREATIVE_TYPE_NOT_SUPPORTED);
        }

        @Test
        void imageFitsMixedPlacementWithoutLengthCheck() {
            EligibilityResult result =
                    CreativeFit.check(VIDEO_AND_IMAGE, 30, CreativeType.IMAGE, null);

            assertThat(result.eligible()).isTrue();
        }
    }

    @Nested
    class Length {

        @Test
        void shorterVideoFits() {
            assertThat(CreativeFit.check(VIDEO_ONLY, 30, CreativeType.VIDEO, 15)
                    .eligible()).isTrue();
        }

        @Test
        void videoExactlyAtTheLimitFits() {
            assertThat(CreativeFit.check(VIDEO_ONLY, 30, CreativeType.VIDEO, 30)
                    .eligible()).isTrue();
        }

        @Test
        void longerVideoDoesNotFit() {
            EligibilityResult result =
                    CreativeFit.check(VIDEO_ONLY, 15, CreativeType.VIDEO, 30);

            assertThat(result.eligible()).isFalse();
            assertThat(result.reasonCode())
                    .isEqualTo(IneligibilityReason.CREATIVE_TOO_LONG);
            assertThat(result.reason()).contains("30s").contains("15s");
        }

        @Test
        void noLimitMeansAnyLengthFits() {
            assertThat(CreativeFit.check(VIDEO_ONLY, null, CreativeType.VIDEO, 120)
                    .eligible()).isTrue();
        }

        @Test
        void videoWithoutDurationIsABug() {
            assertThatThrownBy(() ->
                    CreativeFit.check(VIDEO_ONLY, 30, CreativeType.VIDEO, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ThroughTheRule {

        private final PlacementCompatibilityRule rule = new PlacementCompatibilityRule();

        private CandidateAd candidate(int creativeSeconds, Integer placementMaxSeconds) {
            return new CandidateAd(
                    new CandidateAd.CampaignSnapshot(
                            1L, "Trail Series", CampaignStatus.ACTIVE,
                            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 25),
                            null, null, null, null, null, null, null),
                    new CandidateAd.CreativeSnapshot(
                            1L, CreativeType.VIDEO, CreativeStatus.ACTIVE,
                            CreativeSafetyStatus.APPROVED, creativeSeconds),
                    new CandidateAd.PlacementSnapshot(
                            1L, true, Set.of(DeviceType.TV),
                            VIDEO_ONLY, placementMaxSeconds));
        }

        private AdRequestContext request() {
            return new AdRequestContext(
                    "request-1", 1L, DeviceType.TV, "show-101",
                    "viewer-1", Set.of(), 1, Instant.now());
        }

        @Test
        void thirtySecondVideoIsRejectedFromFifteenSecondSlot() {
            EligibilityResult result = rule.evaluate(candidate(30, 15), request());

            assertThat(result.eligible()).isFalse();
            assertThat(result.reasonCode())
                    .isEqualTo(IneligibilityReason.CREATIVE_TOO_LONG);
        }

        @Test
        void fifteenSecondVideoServesInFifteenSecondSlot() {
            assertThat(rule.evaluate(candidate(15, 15), request()).eligible()).isTrue();
        }
    }
}