package com.adsplatform.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service 
public class StatsService {
    
    static final long MAX_DAYS = 366;
    static final String TOTALS_LABEL = "Total";

    private final StatsSource source;
    private final ZoneId zone;
    private final String billingCurrency;

    public StatsService(
            StatsSource source,
            @Value("${serving.time-zone:America/New_York}") String timeZone,
            @Value("${billing.currency:USD}") String billingCurrency) {
        this.source = source;
        this.zone = ZoneId.of(timeZone);
        this.billingCurrency = billingCurrency;
    }

    public StatsReport report(String advertiser, StatsGrouping grouping, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new IllegalArgumentException("Date range must be at most " + MAX_DAYS + " days");
        }

        List<StatsCounts> counts = source.countsFor(
                new StatsQuery(advertiser, grouping, from, to, zone));

        return new StatsReport(
                advertiser,
                grouping,
                from,
                to,
                zone.getId(),
                billingCurrency,
                counts.stream().map(this::toRow).toList(),
                toRow(sum(counts)));
    }

    private StatsRow toRow(StatsCounts counts) {
        BigDecimal billedValue = counts.conversionValue().get(billingCurrency);

        return new StatsRow(
                counts.key(),
                counts.label(),
                counts.impressions(),
                counts.clicks(),
                counts.spend(),
                counts.conversions(),
                counts.cancelledConversions(),
                ratio(BigDecimal.valueOf(counts.clicks()), BigDecimal.valueOf(counts.impressions()), 4),
                ratio(counts.spend(), BigDecimal.valueOf(counts.clicks()), 2),
                ratio(counts.spend(), BigDecimal.valueOf(counts.conversions()), 2),
                counts.conversionValue(),
                billedValue == null ? null : ratio(billedValue, counts.spend(), 2));
    }

    // Adds every row's counts and every currency's value.
    private static StatsCounts sum(List<StatsCounts> counts) {
        Map<String, BigDecimal> values = new TreeMap<>();
        for (StatsCounts row : counts) {
            row.conversionValue().forEach((currency, value) -> values.merge(currency, value, BigDecimal::add));
        }

        return new StatsCounts(
                null,
                TOTALS_LABEL,
                counts.stream().mapToLong(StatsCounts::impressions).sum(),
                counts.stream().mapToLong(StatsCounts::clicks).sum(),
                counts.stream().map(StatsCounts::spend).reduce(BigDecimal.ZERO, BigDecimal::add),
                counts.stream().mapToLong(StatsCounts::conversions).sum(),
                counts.stream().mapToLong(StatsCounts::cancelledConversions).sum(),
                values);
    }

    // Null when there is nothing to divide by.
    private static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator, int scale) {
        if (denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, scale, RoundingMode.HALF_UP);
    }
}
