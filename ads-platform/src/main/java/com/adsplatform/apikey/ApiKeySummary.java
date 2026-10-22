package com.adsplatform.apikey;

import java.time.Instant;

/*
 * A key as ops can see it after it was issued: identified by its
 * prefix, never by the key or its hash.
 */
public record ApiKeySummary(
        Long id,
        String advertiser,
        String keyPrefix,
        Instant createdAt,
        Instant revokedAt,
        boolean active) {

    static ApiKeySummary from(AdvertiserApiKey key) {
        return new ApiKeySummary(
                key.getId(),
                key.getAdvertiser(),
                key.getKeyPrefix(),
                key.getCreatedAt(),
                key.getRevokedAt(),
                key.isActive());
    }
}