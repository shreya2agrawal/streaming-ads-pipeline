package com.adsplatform.placement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.adsplatform.placement.dto.CreatePlacementRequest;
import com.adsplatform.placement.dto.UpdatePlacementRequest;

@WebMvcTest(PlacementController.class)
class PlacementControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PlacementService placementService;

        @Test
        void shouldActivatePlacement() throws Exception {
                Placement placement = Placement.builder()
                                .id(1L)
                                .name("Mid-Roll")
                                .type(PlacementType.MID_ROLL)
                                .active(true)
                                .build();

                when(placementService.updatePlacementActiveStatus(1L, true))
                                .thenReturn(Optional.of(placement));

                mockMvc.perform(
                                patch("/api/placements/1/active")
                                                .param("active", "true"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andExpect(jsonPath("$.data.active").value(true));
        }

        @Test
        void shouldReturnNotFoundWhenPlacementDoesNotExist() throws Exception {
                when(placementService.updatePlacementActiveStatus(99L, true))
                                .thenReturn(Optional.empty());

                mockMvc.perform(
                                patch("/api/placements/99/active")
                                                .param("active", "true"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldUpdatePlacement() throws Exception {
                Placement updatedPlacement = Placement.builder()
                                .id(1L)
                                .name("Prime Mid-Roll")
                                .type(PlacementType.MID_ROLL)
                                .maxAds(2)
                                .durationSeconds(30)
                                .active(false)
                                .build();

                when(placementService.updatePlacement(
                                eq(1L),
                                any(UpdatePlacementRequest.class)))
                                .thenReturn(Optional.of(updatedPlacement));

                mockMvc.perform(
                                put("/api/placements/1")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "name": "Prime Mid-Roll",
                                                                  "type": "MID_ROLL",
                                                                  "maxAds": 2,
                                                                  "durationSeconds": 30,
                                                                  "supportedDevices": ["TV", "WEB"],
                                                                  "supportedCreativeTypes": ["VIDEO"]
                                                                }
                                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andExpect(jsonPath("$.data.name").value("Prime Mid-Roll"))
                                .andExpect(jsonPath("$.data.type").value("MID_ROLL"));
        }

        @Test
        void shouldReturnNotFoundWhenUpdatingMissingPlacement() throws Exception {
                when(placementService.updatePlacement(
                                eq(99L),
                                any(UpdatePlacementRequest.class)))
                                .thenReturn(Optional.empty());

                mockMvc.perform(
                                put("/api/placements/99")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "name": "Prime Mid-Roll",
                                                                  "type": "MID_ROLL",
                                                                  "maxAds": 2,
                                                                  "durationSeconds": 30,
                                                                  "supportedDevices": ["TV"],
                                                                  "supportedCreativeTypes": ["VIDEO"]
                                                                }
                                                                """))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldCreatePlacement() throws Exception {
                Placement createdPlacement = Placement.builder()
                                .id(1L)
                                .name("Pre-Roll")
                                .type(PlacementType.PRE_ROLL)
                                .maxAds(1)
                                .durationSeconds(30)
                                .active(false)
                                .build();

                when(placementService.createPlacement(any(CreatePlacementRequest.class)))
                                .thenReturn(createdPlacement);

                mockMvc.perform(
                                post("/api/placements")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "name": "Pre-Roll",
                                                                  "type": "PRE_ROLL",
                                                                  "maxAds": 1,
                                                                  "durationSeconds": 30,
                                                                  "supportedDevices": ["TV", "WEB"],
                                                                  "supportedCreativeTypes": ["VIDEO"]
                                                                }
                                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andExpect(jsonPath("$.data.name").value("Pre-Roll"))
                                .andExpect(jsonPath("$.data.type").value("PRE_ROLL"))
                                .andExpect(jsonPath("$.data.active").value(false));
        }

        @Test
        void shouldRejectInvalidPlacementRequest() throws Exception {
                mockMvc.perform(
                                post("/api/placements")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "name": "",
                                                                  "type": "PRE_ROLL",
                                                                  "maxAds": 1,
                                                                  "durationSeconds": 30,
                                                                  "supportedDevices": [],
                                                                  "supportedCreativeTypes": ["VIDEO"]
                                                                }
                                                                """))
                                .andExpect(status().isBadRequest());

                verify(placementService, never())
                                .createPlacement(any(CreatePlacementRequest.class));
        }
}