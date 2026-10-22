package com.adsplatform.stats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.adsplatform.apikey.ApiKeyService;
import com.adsplatform.common.exception.UnauthorizedException;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    private static final String KEY = "ak_test";
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void validKey() {
        when(apiKeyService.authenticate(KEY)).thenReturn("IKEA");
    }

    private static StatsReport report(StatsGrouping grouping) {
        StatsRow row = new StatsRow("10", "Home Refresh Campaign", 6, 5, new BigDecimal("7.50"),
                1, 2, new BigDecimal("0.8333"), new BigDecimal("1.50"), new BigDecimal("7.50"),
                Map.of("USD", new BigDecimal("1800.00")), new BigDecimal("240.00"));
        return new StatsReport("IKEA", grouping, OCT_1, OCT_31, "America/New_York", "USD",
                List.of(row), row);
    }

    @Test
    void statsAreReturnedForTheKeysAdvertiserByCampaignByDefault() throws Exception {
        when(statsService.report("IKEA", StatsGrouping.CAMPAIGN, OCT_1, OCT_31))
                .thenReturn(report(StatsGrouping.CAMPAIGN));

        mockMvc.perform(get("/api/v1/advertiser/stats")
                        .header("X-Api-Key", KEY)
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.advertiser").value("IKEA"))
                .andExpect(jsonPath("$.data.groupBy").value("CAMPAIGN"))
                .andExpect(jsonPath("$.data.rows[0].clicks").value(5))
                .andExpect(jsonPath("$.data.rows[0].conversionValue.USD").value(1800.00))
                .andExpect(jsonPath("$.data.rows[0].returnOnAdSpend").value(240.00));
    }

    @Test
    void groupByIsReadInAnyLetterCase() throws Exception {
        when(statsService.report("IKEA", StatsGrouping.DAY, OCT_1, OCT_31))
                .thenReturn(report(StatsGrouping.DAY));

        mockMvc.perform(get("/api/v1/advertiser/stats")
                        .header("X-Api-Key", KEY)
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("groupBy", "Day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBy").value("DAY"));
    }

    @Test
    void unknownGroupByReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/advertiser/stats")
                        .header("X-Api-Key", KEY)
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("groupBy", "country"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("groupBy must be one of: campaign, day, creative, placement"));

        verify(statsService, never()).report(anyString(), any(), any(), any());
    }

    @Test
    void missingKeyReturns401() throws Exception {
        when(apiKeyService.authenticate(null))
                .thenThrow(new UnauthorizedException("API key required"));

        mockMvc.perform(get("/api/v1/advertiser/stats")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("API key required"));

        verify(statsService, never()).report(anyString(), any(), any(), any());
    }

    @Test
    void invalidRangeReturns400() throws Exception {
        when(statsService.report("IKEA", StatsGrouping.CAMPAIGN, OCT_31, OCT_1))
                .thenThrow(new IllegalArgumentException("'to' must not be before 'from'"));

        mockMvc.perform(get("/api/v1/advertiser/stats")
                        .header("X-Api-Key", KEY)
                        .param("from", "2026-10-31")
                        .param("to", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'to' must not be before 'from'"));
    }
}