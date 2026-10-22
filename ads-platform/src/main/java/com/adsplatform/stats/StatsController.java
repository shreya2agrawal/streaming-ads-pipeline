package com.adsplatform.stats;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adsplatform.apikey.ApiKeyService;
import com.adsplatform.common.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/advertiser/stats")
@RequiredArgsConstructor
public class StatsController {

    static final String API_KEY_HEADER = "X-Api-Key";

    private final StatsService statsService;
    private final ApiKeyService apiKeyService;

    @GetMapping 
    public ResponseEntity<ApiResponse<StatsReport>> stats(
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "campaign") String groupBy) {

        String advertiser = apiKeyService.authenticate(apiKey);
        return ResponseEntity.ok(ApiResponse.ok(
                statsService.report(advertiser, parseGrouping(groupBy), from, to)));
    }

    private static StatsGrouping parseGrouping(String groupBy) {
        try {
            return StatsGrouping.valueOf(groupBy.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "groupBy must be one of: campaign, day, creative, placement");
        }
    }
}
