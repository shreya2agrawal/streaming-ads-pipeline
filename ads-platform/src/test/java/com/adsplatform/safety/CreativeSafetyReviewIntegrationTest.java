package com.adsplatform.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CreativeSafetyReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CreativeRepository creativeRepository;

    @Autowired
    private CreativeSafetyReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCompleteCreativeSafetyReviewFlow() throws Exception {

        Campaign campaign = campaignRepository.save(
                Campaign.builder()
                        .name("Integration Test Campaign")
                        .advertiser("Test Advertiser")
                        .agency("Test Agency")
                        .budget(new BigDecimal("10000"))
                        .flightStart(LocalDate.now())
                        .flightEnd(LocalDate.now().plusDays(30))
                        .status(Campaign.CampaignStatus.DRAFT)
                        .build());

        Creative creative = creativeRepository.save(
                Creative.builder()
                        .campaign(campaign)
                        .name("Integration Test Creative")
                        .creativeType(CreativeType.VIDEO)
                        .mediaUrl("https://cdn.example.com/test.mp4")
                        .destinationUrl("https://example.com")
                        .contentText("Integration test creative")
                        .durationSeconds(30)
                        .status(CreativeStatus.DRAFT)
                        .safetyStatus(CreativeSafetyStatus.UNREVIEWED)
                        .build());

        String submitResponse = mockMvc.perform(
                        post("/api/safety-reviews/creatives/{creativeId}",
                                creative.getId()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode submitJson =
                objectMapper.readTree(submitResponse);

        Long reviewId =
                submitJson.get("data").get("id").asLong();

        Creative pendingCreative =
                creativeRepository.findById(creative.getId()).orElseThrow();

        assertEquals(
                CreativeSafetyStatus.PENDING_REVIEW,
                pendingCreative.getSafetyStatus());

        String requestBody = """
                {
                  "status": "APPROVED",
                  "reviewer": "Integration Tester",
                  "reason": "Creative complies with policy"
                }
                """;

        mockMvc.perform(
                        patch("/api/safety-reviews/{reviewId}", reviewId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk());

        Creative approvedCreative =
                creativeRepository.findById(creative.getId()).orElseThrow();

        CreativeSafetyReview review =
                reviewRepository.findById(reviewId).orElseThrow();

        assertEquals(
                CreativeSafetyStatus.APPROVED,
                approvedCreative.getSafetyStatus());

        assertEquals(
                SafetyReviewStatus.APPROVED,
                review.getStatus());

        assertEquals(
                "Integration Tester",
                review.getReviewer());
    }
}