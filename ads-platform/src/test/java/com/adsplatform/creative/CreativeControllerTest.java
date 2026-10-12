package com.adsplatform.creative;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.dto.CreateCreativeRequest;
import com.adsplatform.creative.dto.CreativeResponse;
import com.adsplatform.creative.dto.UpdateCreativeRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CreativeController.class)
class CreativeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreativeService creativeService;

    @Test
    void shouldGetAllCreatives() throws Exception {

        CreativeResponse creative = creativeResponse();

        when(creativeService.getAllCreatives())
                .thenReturn(List.of(creative));

        mockMvc.perform(get("/api/creatives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(101))
                .andExpect(jsonPath("$.data[0].campaignId").value(7))
                .andExpect(jsonPath("$.data[0].name")
                        .value("Sports Pre-Roll"))
                .andExpect(jsonPath("$.data[0].status")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.data[0].safetyStatus")
                        .value("UNREVIEWED"));
    }

    @Test
    void shouldGetCreativeById() throws Exception {

        when(creativeService.getCreative(101L))
                .thenReturn(creativeResponse());

        mockMvc.perform(get("/api/creatives/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.campaignId").value(7));
    }

    @Test
    void shouldReturn404WhenCreativeDoesNotExist() throws Exception {

        when(creativeService.getCreative(999L))
                .thenThrow(new ResourceNotFoundException(
                        "Creative not found: 999"));

        mockMvc.perform(get("/api/creatives/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Creative not found: 999"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldGetCreativesByCampaign() throws Exception {

        when(creativeService.getCreativesByCampaign(7L))
                .thenReturn(List.of(creativeResponse()));

        mockMvc.perform(get("/api/creatives/campaign/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].campaignId")
                        .value(7));
    }

    @Test
    void shouldCreateCreative() throws Exception {

        when(creativeService.createCreative(
                any(CreateCreativeRequest.class)))
                .thenReturn(creativeResponse());

        String requestBody = """
                {
                  "campaignId": 7,
                  "name": "Sports Pre-Roll",
                  "creativeType": "VIDEO",
                  "mediaUrl": "https://cdn.example.com/sports.mp4",
                  "destinationUrl": "https://advertiser.example.com",
                  "contentText": "Train smarter this summer",
                  "durationSeconds": 30
                }
                """;

        mockMvc.perform(post("/api/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.status")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.data.safetyStatus")
                        .value("UNREVIEWED"));
    }

    @Test
    void shouldRejectInvalidCreateRequest() throws Exception {

        String requestBody = """
                {
                  "campaignId": 7,
                  "name": "",
                  "creativeType": "VIDEO",
                  "mediaUrl": "https://cdn.example.com/sports.mp4",
                  "destinationUrl": "https://advertiser.example.com",
                  "contentText": "Train smarter this summer",
                  "durationSeconds": 30
                }
                """;

        mockMvc.perform(post("/api/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Creative name is required"));

        verify(creativeService, never())
                .createCreative(any(CreateCreativeRequest.class));
    }

    @Test
    void shouldReturn400ForVideoWithoutDuration() throws Exception {

        when(creativeService.createCreative(
                any(CreateCreativeRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Duration is required for video creatives"));

        String requestBody = """
                {
                  "campaignId": 7,
                  "name": "Sports Pre-Roll",
                  "creativeType": "VIDEO",
                  "mediaUrl": "https://cdn.example.com/sports.mp4",
                  "destinationUrl": "https://advertiser.example.com",
                  "contentText": "Train smarter this summer",
                  "durationSeconds": null
                }
                """;

        mockMvc.perform(post("/api/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Duration is required for video creatives"));
    }

    @Test
    void shouldUpdateCreative() throws Exception {

        CreativeResponse updatedCreative =
                new CreativeResponse(
                        101L,
                        7L,
                        "Sports Pre-Roll V2",
                        CreativeType.VIDEO,
                        "https://cdn.example.com/sports-v2.mp4",
                        "https://advertiser.example.com/new",
                        "Updated creative content",
                        15,
                        CreativeStatus.ACTIVE,
                        CreativeSafetyStatus.UNREVIEWED,
                        null,
                        null);

        when(creativeService.updateCreative(
                any(Long.class),
                any(UpdateCreativeRequest.class)))
                .thenReturn(updatedCreative);

        String requestBody = """
                {
                  "name": "Sports Pre-Roll V2",
                  "creativeType": "VIDEO",
                  "mediaUrl": "https://cdn.example.com/sports-v2.mp4",
                  "destinationUrl": "https://advertiser.example.com/new",
                  "contentText": "Updated creative content",
                  "durationSeconds": 15
                }
                """;

        mockMvc.perform(put("/api/creatives/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name")
                        .value("Sports Pre-Roll V2"))
                .andExpect(jsonPath("$.data.durationSeconds")
                        .value(15))
                .andExpect(jsonPath("$.data.safetyStatus")
                        .value("UNREVIEWED"));
    }

    @Test
    void shouldUpdateCreativeStatus() throws Exception {

        CreativeResponse activeCreative =
                new CreativeResponse(
                        101L,
                        7L,
                        "Sports Pre-Roll",
                        CreativeType.VIDEO,
                        "https://cdn.example.com/sports.mp4",
                        "https://advertiser.example.com",
                        "Train smarter this summer",
                        30,
                        CreativeStatus.ACTIVE,
                        CreativeSafetyStatus.APPROVED,
                        null,
                        null);

        when(creativeService.updateStatus(
                101L,
                CreativeStatus.ACTIVE))
                .thenReturn(activeCreative);

        mockMvc.perform(
                        patch("/api/creatives/101/status")
                                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.data.safetyStatus")
                        .value("APPROVED"));
    }

    private CreativeResponse creativeResponse() {
        return new CreativeResponse(
                101L,
                7L,
                "Sports Pre-Roll",
                CreativeType.VIDEO,
                "https://cdn.example.com/sports.mp4",
                "https://advertiser.example.com",
                "Train smarter this summer",
                30,
                CreativeStatus.DRAFT,
                CreativeSafetyStatus.UNREVIEWED,
                null,
                null);
    }
}