package com.adsplatform.serving.conversion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adsplatform.apikey.ApiKeyService;
import com.adsplatform.common.ApiResponse;
import com.adsplatform.serving.conversion.ConversionService.CancelOutcome;
import com.adsplatform.serving.conversion.ConversionService.RecordOutcome;
import com.adsplatform.serving.conversion.dto.CancellationRequest;
import com.adsplatform.serving.conversion.dto.ConversionRequest;
import com.adsplatform.serving.conversion.dto.ConversionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * Server-to-server: the advertiser's server reports bookings and
 * cancellations. No @CrossOrigin, since browsers on other sites
 * have no reason to call this.
 *
 * Designed, not built: authenticating each advertiser with an API key.
 */
@RestController
@RequestMapping("/api/v1/conversions")
@RequiredArgsConstructor
public class ConversionController {

        static final String API_KEY_HEADER = "X-Api-Key";

        private final ConversionService conversionService;
        private final ApiKeyService apiKeyService;

        @PostMapping
        public ResponseEntity<ApiResponse<ConversionResponse>> record(
                        @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey,
                        @Valid @RequestBody ConversionRequest request) {

                String advertiser = apiKeyService.authenticate(apiKey);

                RecordOutcome outcome = conversionService.recordConversion(
                                advertiser,
                                request.clickToken(),
                                request.bookingId(),
                                request.value(),
                                request.currency(),
                                request.convertedAt());

                // 201 only when a new conversion was created; retries get 200.
                HttpStatus status = outcome == RecordOutcome.RECORDED
                                ? HttpStatus.CREATED
                                : HttpStatus.OK;

                return ResponseEntity.status(status)
                                .body(ApiResponse.ok(new ConversionResponse(outcome.name())));
        }

        @PostMapping("/cancellations")
        public ResponseEntity<ApiResponse<ConversionResponse>> cancel(
                        @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey,
                        @Valid @RequestBody CancellationRequest request) {

                String advertiser = apiKeyService.authenticate(apiKey);

                CancelOutcome outcome = conversionService.cancelConversion(
                                advertiser,
                                request.clickToken(),
                                request.bookingId());

                return ResponseEntity.ok(
                                ApiResponse.ok(new ConversionResponse(outcome.name())));
        }

}
