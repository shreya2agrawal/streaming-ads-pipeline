package com.adsplatform.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.common.exception.UnauthorizedException;

class ApiKeyServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    private AdvertiserApiKeyRepository repository;
    private CampaignRepository campaignRepository;
    private ApiKeyService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdvertiserApiKeyRepository.class);
        campaignRepository = mock(CampaignRepository.class);
        when(repository.save(any(AdvertiserApiKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(campaignRepository.existsByAdvertiser("Home Refresh Co.")).thenReturn(true);
        service = new ApiKeyService(repository, campaignRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AdvertiserApiKey storedKey(String rawKey) {
        return AdvertiserApiKey.issue(
                "Home Refresh Co.", "ak_Xq3v9Lm", ApiKeyService.sha256Hex(rawKey), NOW);
    }

    // Issuing

    @Test
    void issuedKeyIsReturnedButOnlyItsHashIsStored() {
        IssuedKey issued = service.issue("Home Refresh Co.");

        ArgumentCaptor<AdvertiserApiKey> saved = ArgumentCaptor.forClass(AdvertiserApiKey.class);
        verify(repository).save(saved.capture());
        AdvertiserApiKey stored = saved.getValue();

        assertThat(issued.apiKey()).startsWith("ak_").hasSize(46);
        assertThat(stored.getKeyHash())
                .hasSize(64)
                .isEqualTo(ApiKeyService.sha256Hex(issued.apiKey()))
                .isNotEqualTo(issued.apiKey());
        assertThat(stored.getKeyPrefix()).isEqualTo(issued.apiKey().substring(0, 11));
        assertThat(stored.getAdvertiser()).isEqualTo("Home Refresh Co.");
        assertThat(stored.getCreatedAt()).isEqualTo(NOW);
        assertThat(stored.isActive()).isTrue();
    }

    @Test
    void everyIssuedKeyIsDifferent() {
        IssuedKey first = service.issue("Home Refresh Co.");
        IssuedKey second = service.issue("Home Refresh Co.");

        assertThat(first.apiKey()).isNotEqualTo(second.apiKey());
    }

    @Test
    void keyIsNotIssuedForAnAdvertiserWithNoCampaigns() {
        when(campaignRepository.existsByAdvertiser("Home Refresh Co")).thenReturn(false);

        assertThatThrownBy(() -> service.issue("Home Refresh Co"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No campaigns for advertiser: Home Refresh Co");

        verify(repository, never()).save(any());
    }

    // Revoking

    @Test
    void revokedKeyBecomesInactive() {
        AdvertiserApiKey key = storedKey("ak_secret");
        when(repository.findById(7L)).thenReturn(Optional.of(key));

        ApiKeySummary summary = service.revoke(7L);

        assertThat(summary.active()).isFalse();
        assertThat(summary.revokedAt()).isEqualTo(NOW);
    }

    @Test
    void revokingAnUnknownKeyIsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("API key not found: 99");
    }

    // Checking

    @Test
    void activeKeyIdentifiesItsAdvertiser() {
        when(repository.findByKeyHash(ApiKeyService.sha256Hex("ak_secret")))
                .thenReturn(Optional.of(storedKey("ak_secret")));

        assertThat(service.authenticate("ak_secret")).isEqualTo("Home Refresh Co.");
    }

    @Test
    void missingKeyIsRejected() {
        assertThatThrownBy(() -> service.authenticate(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("API key required");

        assertThatThrownBy(() -> service.authenticate("   "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("API key required");
    }

    @Test
    void unknownKeyIsRejected() {
        when(repository.findByKeyHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate("ak_made_up"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid API key");
    }

    @Test
    void revokedKeyIsRejectedLikeAnUnknownOne() {
        AdvertiserApiKey key = storedKey("ak_secret");
        key.revoke(NOW);
        when(repository.findByKeyHash(ApiKeyService.sha256Hex("ak_secret")))
                .thenReturn(Optional.of(key));

        assertThatThrownBy(() -> service.authenticate("ak_secret"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid API key");
    }

    @Test
    void sha256MatchesTheStandardTestValue() {
        // The published SHA-256 test vector for "abc".
        assertThat(ApiKeyService.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}