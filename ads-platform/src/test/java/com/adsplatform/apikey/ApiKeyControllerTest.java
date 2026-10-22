package com.adsplatform.apikey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiKeyController.class)
class ApiKeyControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @Test
    void issuingReturns201WithTheKeyOnce() throws Exception {
        when(apiKeyService.issue("Home Refresh Co."))
                .thenReturn(new IssuedKey(7L, "Home Refresh Co.", "ak_Xq3v9Lm", "ak_Xq3v9Lm2full", NOW));

        mockMvc.perform(post("/api/v1/advertiser-api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "advertiser": "Home Refresh Co." }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.apiKey").value("ak_Xq3v9Lm2full"))
                .andExpect(jsonPath("$.data.keyPrefix").value("ak_Xq3v9Lm"));
    }

    @Test
    void blankAdvertiserIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/advertiser-api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "advertiser": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Advertiser is required"));

        verify(apiKeyService, never()).issue(any());
    }

    @Test
    void listingNeverShowsTheKeyOrItsHash() throws Exception {
        when(apiKeyService.list("Home Refresh Co."))
                .thenReturn(List.of(new ApiKeySummary(7L, "Home Refresh Co.", "ak_Xq3v9Lm", NOW, null, true)));

        mockMvc.perform(get("/api/v1/advertiser-api-keys")
                        .param("advertiser", "Home Refresh Co."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].keyPrefix").value("ak_Xq3v9Lm"))
                .andExpect(jsonPath("$.data[0].apiKey").doesNotExist())
                .andExpect(jsonPath("$.data[0].keyHash").doesNotExist());
    }

    @Test
    void revokingReturnsTheInactiveKey() throws Exception {
        when(apiKeyService.revoke(7L))
                .thenReturn(new ApiKeySummary(7L, "Home Refresh Co.", "ak_Xq3v9Lm", NOW, NOW, false));

        mockMvc.perform(post("/api/v1/advertiser-api-keys/7/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }
}