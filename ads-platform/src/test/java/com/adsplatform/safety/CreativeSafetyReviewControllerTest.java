package com.adsplatform.safety;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.safety.dto.CreativeSafetyReviewResponse;
import com.adsplatform.safety.dto.SafetyReviewDecisionRequest;

@WebMvcTest(CreativeSafetyReviewController.class)
class CreativeSafetyReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreativeSafetyReviewService reviewService;

    @Test
    void shouldSubmitCreativeForReview() throws Exception {

        when(reviewService.submitForReview(101L))
                .thenReturn(pendingReview());

        mockMvc.perform(
                        post("/api/safety-reviews/creatives/101"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(55))
                .andExpect(jsonPath("$.data.creativeId").value(101))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reviewSource").value("HUMAN"));
    }

    @Test
    void shouldReturn404WhenSubmittingMissingCreative() throws Exception {

        when(reviewService.submitForReview(999L))
                .thenThrow(new ResourceNotFoundException(
                        "Creative not found: 999"));

        mockMvc.perform(
                        post("/api/safety-reviews/creatives/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Creative not found: 999"));
    }

    @Test
    void shouldApproveSafetyReview() throws Exception {

        CreativeSafetyReviewResponse approved =
                new CreativeSafetyReviewResponse(
                        55L,
                        101L,
                        SafetyReviewStatus.APPROVED,
                        SafetyReviewSource.HUMAN,
                        "Creative complies with policy",
                        null,
                        null,
                        null,
                        "Shreya",
                        null,
                        null);

        when(reviewService.decideReview(
                any(Long.class),
                any(SafetyReviewDecisionRequest.class)))
                .thenReturn(approved);

        String requestBody = """
                {
                  "status": "APPROVED",
                  "reviewer": "Shreya",
                  "reason": "Creative complies with policy"
                }
                """;

        mockMvc.perform(
                        patch("/api/safety-reviews/55")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewer").value("Shreya"));
    }

    @Test
    void shouldRejectInvalidDecisionRequest() throws Exception {

        String requestBody = """
                {
                  "status": "APPROVED",
                  "reviewer": "",
                  "reason": "Approved"
                }
                """;

        mockMvc.perform(
                        patch("/api/safety-reviews/55")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(reviewService, never())
                .decideReview(
                        any(Long.class),
                        any(SafetyReviewDecisionRequest.class));
    }

    @Test
    void shouldReturn400WhenDecisionIsPending() throws Exception {

        when(reviewService.decideReview(
                any(Long.class),
                any(SafetyReviewDecisionRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Review decision must be APPROVED or REJECTED"));

        String requestBody = """
                {
                  "status": "PENDING",
                  "reviewer": "Shreya",
                  "reason": "Still reviewing"
                }
                """;

        mockMvc.perform(
                        patch("/api/safety-reviews/55")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Review decision must be APPROVED or REJECTED"));
    }

    @Test
    void shouldGetReviewHistoryForCreative() throws Exception {

        when(reviewService.getReviewsForCreative(101L))
                .thenReturn(List.of(pendingReview()));

        mockMvc.perform(
                        get("/api/safety-reviews/creatives/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(55))
                .andExpect(jsonPath("$.data[0].creativeId").value(101));
    }

    @Test
    void shouldReturn404ForMissingCreativeReviewHistory()
            throws Exception {

        when(reviewService.getReviewsForCreative(999L))
                .thenThrow(new ResourceNotFoundException(
                        "Creative not found: 999"));

        mockMvc.perform(
                        get("/api/safety-reviews/creatives/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Creative not found: 999"));
    }

    private CreativeSafetyReviewResponse pendingReview() {
        return new CreativeSafetyReviewResponse(
                55L,
                101L,
                SafetyReviewStatus.PENDING,
                SafetyReviewSource.HUMAN,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}