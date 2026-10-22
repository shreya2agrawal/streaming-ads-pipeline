package com.adsplatform.apikey;

import java.time.Instant;

public record IssuedKey(
    Long id,
        String advertiser,
        String keyPrefix,
        String apiKey,
        Instant createdAt) {
}
