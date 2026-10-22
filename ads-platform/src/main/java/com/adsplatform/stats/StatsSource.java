package com.adsplatform.stats;

import java.util.List;

/*
 * Where stats come from. Today: PostgreSQL's raw event tables.
 * Later: a daily rollup for past days and live counters for today,
 * behind this same interface.
 */
public interface StatsSource {

    // One entry per row of the requested grouping, in display order.
    List<StatsCounts> countsFor(StatsQuery query);
}