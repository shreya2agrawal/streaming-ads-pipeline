package com.adsplatform.stats;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/*
 * Reads stats straight from the event tables.
 *
 * Each table is totaled on its own first, one row per key, and only
 * those totals are joined. Joining the raw tables together would pair
 * every impression with every click and multiply the counts (fan-out).
 *
 * Spend comes from ad_clicks.charged_amount, the billing records, so
 * reported spend always matches what was charged.
 *
 * The SQL is assembled from fixed fragments chosen by the grouping.
 * Values from the request are always bound as parameters, never
 * inserted into the SQL text.
 */

@Component
@RequiredArgsConstructor
public class PostgresStatsSource implements StatsSource {

    private record Dimension(
            String rows,
            String impressionKey, String impressionJoin,
            String clickKey, String clickJoin,
            String conversionKey, String conversionJoin) {
    }

    private static final Dimension CAMPAIGN = new Dimension(
            "SELECT c.id::text AS key, c.name AS label, LPAD(c.id::text, 20, '0') AS sort "
                    + "FROM campaigns c WHERE c.advertiser = :advertiser",
            "i.campaign_id::text", "",
            "k.campaign_id::text", "",
            "v.campaign_id::text", "");

    private static final Dimension CREATIVE = new Dimension(
            "SELECT cr.id::text AS key, cr.name AS label, LPAD(cr.id::text, 20, '0') AS sort "
                    + "FROM creatives cr JOIN campaigns c ON c.id = cr.campaign_id "
                    + "WHERE c.advertiser = :advertiser",
            "i.creative_id::text", "",
            "k.creative_id::text", "",
            "vk.creative_id::text", "");

    private static final Dimension PLACEMENT = new Dimension(
            "SELECT p.id::text AS key, p.name AS label, LPAD(p.id::text, 20, '0') AS sort "
                    + "FROM placements p",
            "d.placement_id::text", "JOIN ad_decisions d ON d.decision_id = i.decision_id",
            "d.placement_id::text", "JOIN ad_decisions d ON d.decision_id = k.decision_id",
            "d.placement_id::text", "JOIN ad_decisions d ON d.decision_id = vk.decision_id");

    private static final Dimension DAY = new Dimension(
            "SELECT g::date::text AS key, g::date::text AS label, g::date::text AS sort "
                    + "FROM generate_series(CAST(:fromDate AS date), CAST(:toDate AS date), "
                    + "INTERVAL '1 day') AS g",
            "(i.impressed_at AT TIME ZONE CAST(:zone AS text))::date::text", "",
            "(k.clicked_at AT TIME ZONE CAST(:zone AS text))::date::text", "",
            "(v.converted_at AT TIME ZONE CAST(:zone AS text))::date::text", "");

    private static final String COUNTS = """
            WITH dim AS (
                %s
            ),
            imp AS (
                SELECT %s AS key, COUNT(*) AS impressions
                FROM ad_impressions i
                JOIN campaigns c ON c.id = i.campaign_id
                %s
                WHERE c.advertiser = :advertiser
                AND i.impressed_at >= :starts AND i.impressed_at < :ends
                GROUP BY 1
            ),
            clk AS (
                SELECT %s AS key, COUNT(*) AS clicks, SUM(k.charged_amount) AS spend
                FROM ad_clicks k
                JOIN campaigns c ON c.id = k.campaign_id
                %s
                WHERE c.advertiser = :advertiser
                AND k.clicked_at >= :starts AND k.clicked_at < :ends
                GROUP BY 1
            ),
            conv AS (
                SELECT %s AS key,
                    COUNT(*) FILTER (WHERE v.status = 'CONFIRMED') AS conversions,
                    COUNT(*) FILTER (WHERE v.status = 'CANCELLED') AS cancelled
                FROM ad_conversions v
                JOIN campaigns c ON c.id = v.campaign_id
                JOIN ad_clicks vk ON vk.click_token = v.click_token
                %s
                WHERE c.advertiser = :advertiser
                AND v.converted_at >= :starts AND v.converted_at < :ends
                GROUP BY 1
            )
            SELECT dim.key,
                dim.label,
                COALESCE(imp.impressions, 0) AS impressions,
                COALESCE(clk.clicks, 0) AS clicks,
                COALESCE(clk.spend, 0) AS spend,
                COALESCE(conv.conversions, 0) AS conversions,
                COALESCE(conv.cancelled, 0) AS cancelled
            FROM dim
            LEFT JOIN imp ON imp.key = dim.key
            LEFT JOIN clk  ON clk.key  = dim.key
            LEFT JOIN conv ON conv.key = dim.key
            ORDER BY dim.sort
            """;

    private static final String VALUES = """
            SELECT %s AS key, v.currency, SUM(v.value) AS value
            FROM ad_conversions v
            JOIN campaigns c  ON c.id = v.campaign_id
            JOIN ad_clicks vk ON vk.click_token = v.click_token
            %s
            WHERE c.advertiser = :advertiser
            AND v.status = 'CONFIRMED'
            AND v.converted_at >= :starts AND v.converted_at < :ends
            GROUP BY 1, 2
            """;

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<StatsCounts> countsFor(StatsQuery query) {
        Dimension dimension = dimensionFor(query.grouping());
        MapSqlParameterSource params = parameters(query);

        Map<String, Map<String, BigDecimal>> values = new HashMap<>();
        jdbc.query(
                VALUES.formatted(dimension.conversionKey(), dimension.conversionJoin()),
                params,
                row -> {
                    values.computeIfAbsent(row.getString("key"), key -> new TreeMap<>())
                            .put(row.getString("currency").trim(), row.getBigDecimal("value"));

                });

        String counts = COUNTS.formatted(
                dimension.rows(),
                dimension.impressionKey(), dimension.impressionJoin(),
                dimension.clickKey(), dimension.clickJoin(),
                dimension.conversionKey(), dimension.conversionJoin());

        return jdbc.query(counts, params, (row, rowNumber) -> {
            String key = row.getString("key");
            return new StatsCounts(
                    key,
                    row.getString("label"),
                    row.getLong("impressions"),
                    row.getLong("clicks"),
                    row.getBigDecimal("spend"),
                    row.getLong("conversions"),
                    row.getLong("cancelled"),
                    values.getOrDefault(key, Map.of()));
        });
    }

    private static Dimension dimensionFor(StatsGrouping grouping) {
        return switch (grouping) {
            case CAMPAIGN -> CAMPAIGN;
            case CREATIVE -> CREATIVE;
            case PLACEMENT -> PLACEMENT;
            case DAY -> DAY;
        };
    }

    private static MapSqlParameterSource parameters(StatsQuery query) {
        // OffsetDateTime maps to TIMESTAMPTZ exactly, with no session time zone
        // involved.
        return new MapSqlParameterSource()
                .addValue("advertiser", query.advertiser())
                .addValue("starts", OffsetDateTime.ofInstant(query.starts(), ZoneOffset.UTC))
                .addValue("ends", OffsetDateTime.ofInstant(query.ends(), ZoneOffset.UTC))
                .addValue("fromDate", query.from())
                .addValue("toDate", query.to())
                .addValue("zone", query.zone().getId());
    }
}
