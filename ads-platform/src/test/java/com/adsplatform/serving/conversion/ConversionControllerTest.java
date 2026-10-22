package com.adsplatform.serving.conversion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.serving.conversion.ConversionService.CancelOutcome;
import com.adsplatform.serving.conversion.ConversionService.RecordOutcome;

@WebMvcTest(ConversionController.class)
class ConversionControllerTest {

    private static final String VALID_CONVERSION = """
            {
              "clickToken": "token-abc",
              "bookingId": "HR-1001",
              "value": 2400.00,
              "currency": "USD",
              "convertedAt": "2026-10-20T20:31:00Z"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversionService conversionService;

    private void serviceReturns(RecordOutcome outcome) {
        when(conversionService.recordConversion(
                anyString(), anyString(), any(), anyString(), any()))
                .thenReturn(outcome);
    }

    // Recording

    @Test
    void newConversionReturns201() throws Exception {
        serviceReturns(RecordOutcome.RECORDED);

        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CONVERSION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("RECORDED"));

        verify(conversionService).recordConversion(
                eq("token-abc"), eq("HR-1001"), any(), eq("USD"),
                eq(Instant.parse("2026-10-20T20:31:00Z")));
    }

    @Test
    void duplicateReturns200() throws Exception {
        serviceReturns(RecordOutcome.DUPLICATE);

        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CONVERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DUPLICATE"));
    }

    @Test
    void outsideTheWindowReturns200WithItsStatus() throws Exception {
        serviceReturns(RecordOutcome.OUTSIDE_WINDOW);

        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CONVERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OUTSIDE_WINDOW"));
    }

    @Test
    void missingClickTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookingId": "HR-1001",
                                  "value": 2400.00,
                                  "currency": "USD",
                                  "convertedAt": "2026-10-20T20:31:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Click token is required"));

        verify(conversionService, never())
                .recordConversion(anyString(), anyString(), any(), anyString(), any());
    }

    @Test
    void negativeValueIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clickToken": "token-abc",
                                  "bookingId": "HR-1001",
                                  "value": -5.00,
                                  "currency": "USD",
                                  "convertedAt": "2026-10-20T20:31:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Value must not be negative"));
    }

    @Test
    void lowercaseCurrencyIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clickToken": "token-abc",
                                  "bookingId": "HR-1001",
                                  "value": 2400.00,
                                  "currency": "usd",
                                  "convertedAt": "2026-10-20T20:31:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Currency must be a 3-letter code like USD"));
    }

    @Test
    void unknownClickReturns404() throws Exception {
        when(conversionService.recordConversion(
                anyString(), anyString(), any(), anyString(), any()))
                .thenThrow(new ResourceNotFoundException("Unknown click"));

        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CONVERSION))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Unknown click"));
    }

    @Test
    void futureConversionTimeReturns400() throws Exception {
        when(conversionService.recordConversion(
                anyString(), anyString(), any(), anyString(), any()))
                .thenThrow(new IllegalArgumentException("Conversion time is in the future"));

        mockMvc.perform(post("/api/v1/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CONVERSION))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Conversion time is in the future"));
    }

    // Cancelling

    @Test
    void cancellationReturnsWhatHappened() throws Exception {
        when(conversionService.cancelConversion("token-abc", "HR-1001"))
                .thenReturn(CancelOutcome.CANCELLED);

        mockMvc.perform(post("/api/v1/conversions/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "clickToken": "token-abc", "bookingId": "HR-1001" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}