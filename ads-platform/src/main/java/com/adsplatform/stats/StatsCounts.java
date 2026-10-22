package com.adsplatform.stats;

import java.math.BigDecimal;
import java.util.Map;

public record StatsCounts(
    String key,
    String label,
    long impressions,
    long clicks,
    BigDecimal spend,
    long conversions,
    long cancelledConversions,
    Map<String, BigDecimal> conversionValue) {
}
