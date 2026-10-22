package com.adsplatform.stats;

import java.time.LocalDate;
import java.util.List;

/*
 * An advertiser's stats for an inclusive date range, in the platform's
 * time zone, broken down by one grouping, with a totals row.
 *
 * Spend is charged in billingCurrency, so return on ad spend is only
 * computed from conversion value in that same currency.
 */
public record StatsReport(
        String advertiser,
        StatsGrouping groupBy,
        LocalDate from,
        LocalDate to,
        String timeZone,
        String billingCurrency,
        List<StatsRow> rows,
        StatsRow totals) {
}