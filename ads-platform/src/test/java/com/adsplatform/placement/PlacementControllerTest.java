package com.adsplatform.placement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.adsplatform.common.exception.DuplicateResourceException;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.CreativeType;
import com.adsplatform.placement.dto.CreatePlacementRequest;
import com.adsplatform.placement.dto.PlacementResponse;
import com.adsplatform.placement.dto.UpdatePlacementRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PlacementController.class)
class PlacementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlacementService placementService;

    private static PlacementResponse response(boolean active) {
        return new PlacementResponse(
                1L,
                "Pre-roll short",
                PlacementType.PRE_ROLL,
                active,
                2,
                15,
                List.of(DeviceType.TV, DeviceType.MOBILE),
                List.of(CreativeType.VIDEO));
    }

    private static CreatePlacementRequest validCreateRequest() {
        return new CreatePlacementRequest(
                "Pre-roll short",
                PlacementType.PRE_ROLL,
                2,
                15,
                Set.of(DeviceType.TV, DeviceType.MOBILE),
                Set.of(CreativeType.VIDEO));
    }

    private static UpdatePlacementRequest validUpdateRequest() {
        return new UpdatePlacementRequest(
                "Mid-roll standard",
                PlacementType.MID_ROLL,
                3,
                30,
                Set.of(DeviceType.TV),
                Set.of(CreativeType.VIDEO));
    }

    @Nested
    class Read {

        @Test
        void listsPlacements() throws Exception {
            when(placementService.getAllPlacements()).thenReturn(List.of(response(false)));

            mockMvc.perform(get("/api/placements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("Pre-roll short"))
                    .andExpect(jsonPath("$.data[0].maxAdDurationSeconds").value(15));
        }

        @Test
        void returnsPlacementById() throws Exception {
            when(placementService.getPlacementById(1L)).thenReturn(response(false));

            mockMvc.perform(get("/api/placements/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.supportedDevices[0]").value("TV"));
        }

        @Test
        void missingIdReturns404WithMessage() throws Exception {
            when(placementService.getPlacementById(99L))
                    .thenThrow(new ResourceNotFoundException("Placement not found: 99"));

            mockMvc.perform(get("/api/placements/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Placement not found: 99"));
        }

        @Test
        void listsActivePlacements() throws Exception {
            when(placementService.getActivePlacements()).thenReturn(List.of(response(true)));

            mockMvc.perform(get("/api/placements/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].active").value(true));
        }
    }

    @Nested
    class Create {

        @Test
        void validRequestReturns201() throws Exception {
            when(placementService.createPlacement(any(CreatePlacementRequest.class)))
                    .thenReturn(response(false));

            mockMvc.perform(post("/api/placements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.active").value(false));
        }

        @Test
        void blankNameReturns400WithoutCallingService() throws Exception {
            CreatePlacementRequest blankName = new CreatePlacementRequest(
                    "",
                    PlacementType.PRE_ROLL,
                    2,
                    15,
                    Set.of(DeviceType.TV),
                    Set.of(CreativeType.VIDEO));

            mockMvc.perform(post("/api/placements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(blankName)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Placement name is required"));

            verify(placementService, never()).createPlacement(any());
        }

        @Test
        void entityRuleViolationReturns400() throws Exception {
            when(placementService.createPlacement(any(CreatePlacementRequest.class)))
                    .thenThrow(new IllegalArgumentException(
                            "Placements that accept video require a max ad duration"));

            mockMvc.perform(post("/api/placements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Placements that accept video require a max ad duration"));
        }

        /*
         * Fails until GlobalExceptionHandler maps
         * DuplicateResourceException to 409 (next file).
         */
        @Test
        void duplicateNameReturns409() throws Exception {
            when(placementService.createPlacement(any(CreatePlacementRequest.class)))
                    .thenThrow(new DuplicateResourceException(
                            "Placement name already exists: Pre-roll short"));

            mockMvc.perform(post("/api/placements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("Placement name already exists: Pre-roll short"));
        }
    }

    @Nested
    class Update {

        @Test
        void activatesPlacement() throws Exception {
            when(placementService.updatePlacementActiveStatus(1L, true))
                    .thenReturn(response(true));

            mockMvc.perform(patch("/api/placements/1/active").param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.active").value(true));
        }

        @Test
        void updatesPlacement() throws Exception {
            when(placementService.updatePlacement(eq(1L), any(UpdatePlacementRequest.class)))
                    .thenReturn(response(false));

            mockMvc.perform(put("/api/placements/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void updatingMissingIdReturns404() throws Exception {
            when(placementService.updatePlacement(eq(99L), any(UpdatePlacementRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Placement not found: 99"));

            mockMvc.perform(put("/api/placements/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest())))
                    .andExpect(status().isNotFound());
        }
    }
}