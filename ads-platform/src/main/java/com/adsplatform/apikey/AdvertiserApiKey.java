package com.adsplatform.apikey;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "advertiser_api_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvertiserApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advertiser", nullable = false)
    private String advertiser;

    @Column(name = "key_prefix", nullable = false, unique = true)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, columnDefinition = "bpchar(64)")
    private String keyHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    private AdvertiserApiKey(
            String advertiser, String keyPrefix, String keyHash, Instant createdAt) {
        this.advertiser = advertiser;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.createdAt = createdAt;
    }

    static AdvertiserApiKey issue(
            String advertiser, String keyPrefix, String keyHash, Instant createdAt) {
        return new AdvertiserApiKey(advertiser, keyPrefix, keyHash, createdAt);
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    // Revoking twice keeps the first revocation time.
    void revoke(Instant at){
        if(revokedAt == null){
            revokedAt = at;
        }
    }

}
