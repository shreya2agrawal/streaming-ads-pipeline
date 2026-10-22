package com.adsplatform.serving.eligibility.rules;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.eligibility.EligibilityRule;
import com.adsplatform.serving.eligibility.IneligibilityReason;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;

/*
 * Flight dates are inclusive and read in the platform time zone,
 * set as a region like America/New_York so daylight saving works.
 * In UTC, US campaigns would stop in the evening of their last day.
 */

@Component
@Order(20)
public class FlightDateRule implements EligibilityRule {

    static final String DEFAULT_TIME_ZONE = "America/New_York";

    private final ZoneId platformZone;

    /*
     * Used where no Spring configuration exists, e.g. unit tests.
     */
    public FlightDateRule() {
        this(DEFAULT_TIME_ZONE);
    }

    /*
     * An invalid zone fails here, at startup
     */
    @Autowired
    public FlightDateRule(
            @Value("${serving.time-zone:" + DEFAULT_TIME_ZONE + "}")
            String timeZone) {

        this.platformZone = ZoneId.of(timeZone);
    }

    @Override
    public EligibilityResult evaluate(
            CandidateAd candidate,
            AdRequestContext context) {

        LocalDate flightStart =
                candidate.campaign().flightStart();

        LocalDate flightEnd =
                candidate.campaign().flightEnd();

        LocalDate requestDate =
                LocalDate.ofInstant(
                        context.requestedAt(),
                        platformZone
                );

        if (flightStart != null
                && requestDate.isBefore(flightStart)) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.FLIGHT_NOT_STARTED,
                    "Campaign flight has not started"
            );
        }

        if (flightEnd != null
                && requestDate.isAfter(flightEnd)) {

            return EligibilityResult.ineligible(
                    IneligibilityReason.FLIGHT_ENDED,
                    "Campaign flight has ended"
            );
        }

        return EligibilityResult.allowed();
    }
}