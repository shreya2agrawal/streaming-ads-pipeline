package com.adsplatform.apikey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.common.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    static final String KEY_MARKER = "ak_";
    static final int PREFIX_LENGTH = 11; // "ak_" plus 8 characters

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final AdvertiserApiKeyRepository repository;
    private final CampaignRepository campaignRepository;
    private final Clock clock;

    @Transactional
    public IssuedKey issue(String advertiser) {

        if (!campaignRepository.existsByAdvertiser(advertiser)) {
            throw new ResourceNotFoundException("No campaigns for advertiser: " + advertiser);
        }
        
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String apiKey = KEY_MARKER + ENCODER.encodeToString(bytes);

        AdvertiserApiKey saved = repository.save(
                AdvertiserApiKey.issue(
                        advertiser,
                        apiKey.substring(0, PREFIX_LENGTH),
                        sha256Hex(apiKey),
                        clock.instant()));

        return new IssuedKey(
                saved.getId(),
                saved.getAdvertiser(),
                saved.getKeyPrefix(),
                apiKey,
                saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ApiKeySummary> list(String advertiser){
        return repository.findByAdvertiserOrderByCreatedAtDesc(advertiser)
        .stream()
        .map(ApiKeySummary::from)
        .toList();
    }
    
    @Transactional
    public ApiKeySummary revoke(Long id) {
        AdvertiserApiKey key = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + id));
        key.revoke(clock.instant());
        return ApiKeySummary.from(key);
    }


    @Transactional(readOnly = true)
    public String authenticate(String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("API key required");
        }

        return repository.findByKeyHash(sha256Hex(apiKey))
                .filter(AdvertiserApiKey::isActive)
                .map(AdvertiserApiKey::getAdvertiser)
                .orElseThrow(() -> new UnauthorizedException("Invalid API key"));
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Every Java runtime is required to support SHA-256.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
