package com.adsplatform.serving.decision;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adsplatform.common.ApiResponse;
import com.adsplatform.serving.decision.dto.AdDecisionRequest;
import com.adsplatform.serving.decision.dto.AdDecisionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * The serving API: a player asks for ads when a viewer reaches
 * an ad opportunity.
 *
 * Versioned (/api/v1) because players are external clients that
 * cannot all be updated at once when the contract changes.
 *
 * debug=true adds the explanation (scores, prices, exclusions)
 * for testing and the demo; players should not send it.
 */
@RestController
@RequestMapping("/api/v1/ad-decisions")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class AdDecisionController {

    private final AdServingService adServingService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdDecisionResponse>> decide(
            @Valid @RequestBody AdDecisionRequest request,
            @RequestParam(defaultValue = "false") boolean debug) {

        return ResponseEntity.ok(
                ApiResponse.ok(adServingService.serve(request, debug)));
    }
}