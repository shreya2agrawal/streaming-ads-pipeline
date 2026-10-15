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
 * Campaign can serve only during its configured flight window.
 *
 * Start and end dates are inclusive, and are interpreted in the
 * platform time zone. Flight dates carry no zone of their own, so
 * comparing them in UTC would end US campaigns in the evening of
 * their last day and start them the evening before their first.
 *
 * Uses a region ID (e.g. America/New_York), not a fixed offset,
 * so daylight saving time is handled automatically.
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
     * An invalid zone fails here, at startup, rather than on the
     * first ad request.
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