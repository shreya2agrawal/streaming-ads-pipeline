package com.adsplatform.safety;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adsplatform.common.ApiResponse;
import com.adsplatform.safety.dto.CreativeSafetyReviewResponse;
import com.adsplatform.safety.dto.SafetyReviewDecisionRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/safety-reviews")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class CreativeSafetyReviewController {

    private final CreativeSafetyReviewService reviewService;

    @PostMapping("/creatives/{creativeId}")
    public ResponseEntity<ApiResponse<CreativeSafetyReviewResponse>> submitForReview(
            @PathVariable Long creativeId) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        reviewService.submitForReview(creativeId)));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<CreativeSafetyReviewResponse>> decideReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody SafetyReviewDecisionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        reviewService.decideReview(reviewId, request)));
    }

    @GetMapping("/creatives/{creativeId}")
    public ResponseEntity<ApiResponse<List<CreativeSafetyReviewResponse>>> getReviewsForCreative(
            @PathVariable Long creativeId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        reviewService.getReviewsForCreative(creativeId)));
    }
}