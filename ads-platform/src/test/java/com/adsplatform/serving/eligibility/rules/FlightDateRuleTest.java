package com.adsplatform.serving.eligibility.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.adsplatform.campaign.Campaign.CampaignStatus;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.DeviceType;
import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

class FlightDateRuleTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    // Flight: Oct 1 to Nov 25, 2026, inclusive.
    private static CandidateAd candidate() {
        return new CandidateAd(
                new CandidateAd.CampaignSnapshot(
                        1L,
                        "Trail Series",
                        CampaignStatus.ACTIVE,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 25),
                        null, null, null,
                        null, null, null, null),
                new CandidateAd.CreativeSnapshot(
                        1L,
                        CreativeType.VIDEO,
                        CreativeStatus.ACTIVE,
                        CreativeSafetyStatus.APPROVED,
                        15),
                new CandidateAd.PlacementSnapshot(
                        1L,
                        true,
                        Set.of(DeviceType.TV),
                        Set.of(CreativeType.VIDEO)));
    }

    private static AdRequestContext requestAt(Instant instant) {
        return new AdRequestContext(
                "request-1",
                1L,
                DeviceType.TV,
                "show-101",
                "viewer-1",
                Set.of(),
                1,
                instant);
    }

    private static Instant newYork(int month, int day, int hour, int minute) {
        return ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, NEW_YORK)
                .toInstant();
    }

    @Test
    void servesOnTheEveningOfTheLastDay() {
        // 7:30 pm Nov 25 in New York is already Nov 26 in UTC.
        EligibilityResult result = new FlightDateRule()
                .evaluate(candidate(), requestAt(newYork(11, 25, 19, 30)));

        assertThat(result.eligible()).isTrue();
    }

    @Test
    void stopsAfterMidnightOnTheLastDay() {
        EligibilityResult result = new FlightDateRule()
                .evaluate(candidate(), requestAt(newYork(11, 26, 0, 30)));

        assertThat(result.eligible()).isFalse();
        assertThat(result.reasonCode()).isEqualTo(IneligibilityReason.FLIGHT_ENDED);
    }

    @Test
    void doesNotStartTheEveningBeforeTheFirstDay() {
        // 11:30 pm Sep 30 in New York is already Oct 1 in UTC.
        EligibilityResult result = new FlightDateRule()
                .evaluate(candidate(), requestAt(newYork(9, 30, 23, 30)));

        assertThat(result.eligible()).isFalse();
        assertThat(result.reasonCode()).isEqualTo(IneligibilityReason.FLIGHT_NOT_STARTED);
    }

    @Test
    void servesFromMidnightOnTheFirstDay() {
        EligibilityResult result = new FlightDateRule()
                .evaluate(candidate(), requestAt(newYork(10, 1, 0, 30)));

        assertThat(result.eligible()).isTrue();
    }

    @Test
    void sameInstantGivesDifferentAnswersInDifferentZones() {
        // 00:30 Nov 26 in New York is 9:30 pm Nov 25 in Los Angeles.
        Instant instant = newYork(11, 26, 0, 30);

        EligibilityResult newYorkResult = new FlightDateRule("America/New_York")
                .evaluate(candidate(), requestAt(instant));

        EligibilityResult losAngelesResult = new FlightDateRule("America/Los_Angeles")
                .evaluate(candidate(), requestAt(instant));

        assertThat(newYorkResult.eligible()).isFalse();
        assertThat(losAngelesResult.eligible()).isTrue();
    }

    @Test
    void invalidTimeZoneFailsImmediately() {
        assertThatThrownBy(() -> new FlightDateRule("Not/A_Zone"))
                .isInstanceOf(DateTimeException.class);
    }
}