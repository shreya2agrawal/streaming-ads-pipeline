package com.adsplatform.stats;

import java.math.BigDecimal;
import java.util.Map;

public record StatsRow(
        String key,
        String label,
        long impressions,
        long clicks,
        BigDecimal spend,
        long conversions,
        long cancelledConversions,
        BigDecimal ctr,
        BigDecimal averageCpc,
        BigDecimal costPerConversion,
        Map<String, BigDecimal> conversionValue,
        BigDecimal returnOnAdSpend) {
}
