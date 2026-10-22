package com.adsplatform.stats;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/*
 * What a StatsSource is asked for: one advertiser's events between two
 * dates (inclusive), in the platform's time zone, broken down one way.
 *
 * The dates become the half-open range [from 00:00, to + 1 day 00:00).
 */

public record StatsQuery(
    String advertiser,
    StatsGrouping grouping,
    LocalDate from,
    LocalDate to,
    ZoneId zone
) {
    public Instant starts(){
        return from.atStartOfDay(zone).toInstant();
    }

    public Instant ends(){
        return to.plusDays(1).atStartOfDay(zone).toInstant();
    }
}
