package com.adsplatform.safety;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.safety.dto.CreativeSafetyReviewResponse;
import com.adsplatform.safety.dto.SafetyReviewDecisionRequest;

@Service
public class CreativeSafetyReviewService {

    private final CreativeSafetyReviewRepository reviewRepository;
    private final CreativeRepository creativeRepository;

    public CreativeSafetyReviewService(
            CreativeSafetyReviewRepository reviewRepository,
            CreativeRepository creativeRepository) {
        this.reviewRepository = reviewRepository;
        this.creativeRepository = creativeRepository;
    }

    @Transactional
    public CreativeSafetyReviewResponse submitForReview(Long creativeId) {

        Creative creative = creativeRepository.findById(creativeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creative not found: " + creativeId));

        if (creative.getSafetyStatus() != CreativeSafetyStatus.UNREVIEWED) {
            throw new IllegalArgumentException(
                    "Only unreviewed creatives can be submitted for safety review");
        }

        creative.setSafetyStatus(CreativeSafetyStatus.PENDING_REVIEW);
        creativeRepository.save(creative);

        CreativeSafetyReview review = CreativeSafetyReview.builder()
                .creative(creative)
                .status(SafetyReviewStatus.PENDING)
                .reviewSource(SafetyReviewSource.HUMAN)
                .build();

        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public CreativeSafetyReviewResponse decideReview(
            Long reviewId,
            SafetyReviewDecisionRequest request) {

        if (request.status() == SafetyReviewStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Review decision must be APPROVED or REJECTED");
        }

        CreativeSafetyReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Safety review not found: " + reviewId));

        if (review.getStatus() != SafetyReviewStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Safety review is already completed: " + reviewId);
        }

        review.setStatus(request.status());
        review.setReviewer(request.reviewer());
        review.setReason(request.reason());
        review.setReviewedAt(LocalDateTime.now());

        Creative creative = review.getCreative();

        if (request.status() == SafetyReviewStatus.APPROVED) {
            creative.setSafetyStatus(CreativeSafetyStatus.APPROVED);
        } else {
            creative.setSafetyStatus(CreativeSafetyStatus.REJECTED);
        }

        creativeRepository.save(creative);

        return toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<CreativeSafetyReviewResponse> getReviewsForCreative(
            Long creativeId) {

        if (!creativeRepository.existsById(creativeId)) {
            throw new ResourceNotFoundException(
                    "Creative not found: " + creativeId);
        }

        return reviewRepository
                .findByCreative_IdOrderByCreatedAtDesc(creativeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CreativeSafetyReviewResponse toResponse(
            CreativeSafetyReview review) {

        return new CreativeSafetyReviewResponse(
                review.getId(),
                review.getCreative().getId(),
                review.getStatus(),
                review.getReviewSource(),
                review.getReason(),
                review.getScore(),
                review.getModelVersion(),
                review.getPolicyVersion(),
                review.getReviewer(),
                review.getCreatedAt(),
                review.getReviewedAt());
    }
}