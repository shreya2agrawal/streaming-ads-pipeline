package com.adsplatform.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StatsServiceTest {

    private static final String IKEA = "IKEA";
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);

    private StatsSource source;
    private StatsService service;

    @BeforeEach
    void setUp() {
        source = mock(StatsSource.class);
        service = new StatsService(source, "America/New_York", "USD");
    }

    private static StatsCounts counts(String key, long impressions, long clicks, String spend,
            long conversions, Map<String, BigDecimal> values) {
        return new StatsCounts(key, "Row " + key, impressions, clicks, new BigDecimal(spend),
                conversions, 0, values);
    }

    private void sourceReturns(StatsCounts... rows) {
        when(source.countsFor(any())).thenReturn(List.of(rows));
    }

    private StatsReport report() {
        return service.report(IKEA, StatsGrouping.CAMPAIGN, OCT_1, OCT_31);
    }

    // The query

    @Test
    void daysAreNewYorkDaysAsAHalfOpenRange() {
        sourceReturns();

        report();

        ArgumentCaptor<StatsQuery> query = ArgumentCaptor.forClass(StatsQuery.class);
        verify(source).countsFor(query.capture());

        // Midnight in New York (EDT, UTC-4) on Oct 1, up to midnight on Nov 1.
        assertThat(query.getValue().advertiser()).isEqualTo(IKEA);
        assertThat(query.getValue().grouping()).isEqualTo(StatsGrouping.CAMPAIGN);
        assertThat(query.getValue().starts()).isEqualTo(Instant.parse("2026-10-01T04:00:00Z"));
        assertThat(query.getValue().ends()).isEqualTo(Instant.parse("2026-11-01T04:00:00Z"));
    }

    // Ratios

    @Test
    void ratiosAreComputedPerRow() {
        sourceReturns(counts("10", 5, 4, "6.00", 2, Map.of()));

        StatsRow row = report().rows().get(0);

        assertThat(row.ctr()).isEqualByComparingTo("0.8");
        assertThat(row.averageCpc()).isEqualByComparingTo("1.50");
        assertThat(row.costPerConversion()).isEqualByComparingTo("3.00");
    }

    @Test
    void ratiosAreEmptyWhenThereIsNothingToDivideBy() {
        sourceReturns(counts("10", 0, 0, "0", 0, Map.of()));

        StatsRow row = report().rows().get(0);

        assertThat(row.ctr()).isNull();
        assertThat(row.averageCpc()).isNull();
        assertThat(row.costPerConversion()).isNull();
        assertThat(row.returnOnAdSpend()).isNull();
    }

    @Test
    void totalsAreComputedFromSumsNotByAveragingRatios() {
        sourceReturns(
                counts("10", 100, 10, "15.00", 0, Map.of()),   // CTR 0.10
                counts("11", 10, 5, "7.50", 0, Map.of()));     // CTR 0.50

        StatsRow totals = report().totals();

        // Averaging the two CTRs would give 0.30. The true CTR is 15 / 110.
        assertThat(totals.label()).isEqualTo("Total");
        assertThat(totals.impressions()).isEqualTo(110);
        assertThat(totals.clicks()).isEqualTo(15);
        assertThat(totals.ctr()).isEqualByComparingTo("0.1364");
        assertThat(totals.spend()).isEqualByComparingTo("22.50");
    }

    // Conversion value and return on ad spend

    @Test
    void returnOnAdSpendUsesOnlyTheBillingCurrency() {
        sourceReturns(counts("10", 6, 5, "7.50", 2, Map.of(
                "USD", new BigDecimal("1800.00"),
                "VND", new BigDecimal("1000000"))));

        StatsRow row = report().rows().get(0);

        // 1800.00 USD / 7.50 USD of spend. The dong are shown, but not divided by dollars.
        assertThat(row.returnOnAdSpend()).isEqualByComparingTo("240.00");
        assertThat(row.conversionValue())
                .containsEntry("USD", new BigDecimal("1800.00"))
                .containsEntry("VND", new BigDecimal("1000000"));
    }

    @Test
    void returnOnAdSpendIsEmptyWithoutBillingCurrencyValueOrSpend() {
        sourceReturns(
                counts("10", 6, 5, "7.50", 1, Map.of("VND", new BigDecimal("1000000"))),
                counts("11", 6, 0, "0", 1, Map.of("USD", new BigDecimal("500.00"))));

        List<StatsRow> rows = report().rows();

        assertThat(rows.get(0).returnOnAdSpend()).isNull();   // no USD value
        assertThat(rows.get(1).returnOnAdSpend()).isNull();   // no spend
    }

    @Test
    void totalsAddValuesPerCurrency() {
        sourceReturns(
                counts("10", 1, 1, "1.50", 1, Map.of("USD", new BigDecimal("100.00"))),
                counts("11", 1, 1, "1.50", 2, Map.of(
                        "USD", new BigDecimal("50.00"),
                        "EUR", new BigDecimal("20.00"))));

        StatsRow totals = report().totals();

        assertThat(totals.conversionValue())
                .containsEntry("USD", new BigDecimal("150.00"))
                .containsEntry("EUR", new BigDecimal("20.00"))
                .hasSize(2);
        assertThat(totals.returnOnAdSpend()).isEqualByComparingTo("50.00");   // 150.00 / 3.00
    }

    // The range

    @Test
    void endBeforeStartIsRejected() {
        assertThatThrownBy(() -> service.report(IKEA, StatsGrouping.CAMPAIGN, OCT_31, OCT_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'to' must not be before 'from'");
    }

    @Test
    void rangeLongerThanAYearIsRejected() {
        assertThatThrownBy(() -> service.report(IKEA, StatsGrouping.CAMPAIGN, OCT_1, OCT_1.plusDays(366)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Date range must be at most 366 days");
    }
}