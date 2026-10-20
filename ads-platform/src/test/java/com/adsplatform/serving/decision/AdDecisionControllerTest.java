package com.adsplatform.serving.decision;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.serving.decision.dto.AdDecisionRequest;
import com.adsplatform.serving.decision.dto.AdDecisionResponse;

@WebMvcTest(AdDecisionController.class)
class AdDecisionControllerTest {

    private static final String VALID_REQUEST = """
            {
              "placementId": 1,
              "deviceType": "TV",
              "showId": "show-101",
              "availableSlots": 2
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdServingService adServingService;

    private static AdDecisionResponse oneAd() {
        return new AdDecisionResponse(
                UUID.fromString("7f3a0000-0000-0000-0000-000000000001"),
                List.of(new AdDecisionResponse.ServedAd(
                        1, 1L, 11L, CreativeType.VIDEO,
                        "https://cdn.example.com/11.mp4", 15,
                        "/api/v1/clicks/abc123",
                        "/api/v1/impressions/abc123")),
                null,
                null);
    }

    @Test
    void validRequestReturnsTheChosenAds() throws Exception {
        when(adServingService.serve(any(AdDecisionRequest.class), eq(false)))
                .thenReturn(oneAd());

        mockMvc.perform(post("/api/v1/ad-decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.decisionId")
                        .value("7f3a0000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.data.ads[0].clickUrl").value("/api/v1/clicks/abc123"))
                .andExpect(jsonPath("$.data.ads[0].impressionUrl").value("/api/v1/impressions/abc123"))
                .andExpect(jsonPath("$.data.explanation").doesNotExist());
    }

    @Test
    void debugParameterIsPassedThrough() throws Exception {
        when(adServingService.serve(any(AdDecisionRequest.class), eq(true)))
                .thenReturn(oneAd());

        mockMvc.perform(post("/api/v1/ad-decisions")
                        .param("debug", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk());

        verify(adServingService).serve(any(AdDecisionRequest.class), eq(true));
    }

    @Test
    void missingPlacementIsRejectedBeforeServing() throws Exception {
        mockMvc.perform(post("/api/v1/ad-decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "deviceType": "TV", "showId": "show-101", "availableSlots": 1 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Placement id is required"));

        verify(adServingService, never()).serve(any(), any(Boolean.class));
    }

    @Test
    void tooManySlotsIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/ad-decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "placementId": 1,
                                  "deviceType": "TV",
                                  "showId": "show-101",
                                  "availableSlots": 11
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Available slots must be at most 10"));
    }

    @Test
    void unknownPlacementReturns404() throws Exception {
        when(adServingService.serve(any(AdDecisionRequest.class), eq(false)))
                .thenThrow(new ResourceNotFoundException("Placement not found: 1"));

        mockMvc.perform(post("/api/v1/ad-decisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Placement not found: 1"));
    }
}