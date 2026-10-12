package com.adsplatform.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.safety.dto.CreativeSafetyReviewResponse;
import com.adsplatform.safety.dto.SafetyReviewDecisionRequest;

@ExtendWith(MockitoExtension.class)
class CreativeSafetyReviewServiceTest {

    @Mock
    private CreativeSafetyReviewRepository reviewRepository;

    @Mock
    private CreativeRepository creativeRepository;

    private CreativeSafetyReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new CreativeSafetyReviewService(
                reviewRepository,
                creativeRepository);
    }

    @Test
    void shouldSubmitUnreviewedCreativeForReview() {

        Creative creative = creative(
                101L,
                CreativeSafetyStatus.UNREVIEWED);

        when(creativeRepository.findById(101L))
                .thenReturn(Optional.of(creative));

        when(reviewRepository.save(any(CreativeSafetyReview.class)))
                .thenAnswer(invocation -> {
                    CreativeSafetyReview review = invocation.getArgument(0);
                    review.setId(55L);
                    return review;
                });

        CreativeSafetyReviewResponse response =
                reviewService.submitForReview(101L);

        assertEquals(55L, response.id());
        assertEquals(101L, response.creativeId());
        assertEquals(SafetyReviewStatus.PENDING, response.status());
        assertEquals(SafetyReviewSource.HUMAN, response.reviewSource());
        assertEquals(
                CreativeSafetyStatus.PENDING_REVIEW,
                creative.getSafetyStatus());

        verify(creativeRepository).save(creative);
    }

    @Test
    void shouldRejectSubmitWhenCreativeDoesNotExist() {

        when(creativeRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> reviewService.submitForReview(999L));

        assertEquals(
                "Creative not found: 999",
                exception.getMessage());

        verify(reviewRepository, never())
                .save(any(CreativeSafetyReview.class));
    }

    @Test
    void shouldRejectCreativeAlreadySubmittedForReview() {

        Creative creative = creative(
                101L,
                CreativeSafetyStatus.PENDING_REVIEW);

        when(creativeRepository.findById(101L))
                .thenReturn(Optional.of(creative));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> reviewService.submitForReview(101L));

        assertEquals(
                "Only unreviewed creatives can be submitted for safety review",
                exception.getMessage());

        verify(reviewRepository, never())
                .save(any(CreativeSafetyReview.class));
    }

    @Test
    void shouldApprovePendingReview() {

        Creative creative = creative(
                101L,
                CreativeSafetyStatus.PENDING_REVIEW);

        CreativeSafetyReview review =
                review(55L, creative, SafetyReviewStatus.PENDING);

        SafetyReviewDecisionRequest request =
                new SafetyReviewDecisionRequest(
                        SafetyReviewStatus.APPROVED,
                        "Shreya",
                        "Creative complies with policy");

        when(reviewRepository.findById(55L))
                .thenReturn(Optional.of(review));

        when(reviewRepository.save(any(CreativeSafetyReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreativeSafetyReviewResponse response =
                reviewService.decideReview(55L, request);

        assertEquals(SafetyReviewStatus.APPROVED, response.status());
        assertEquals("Shreya", response.reviewer());
        assertEquals(
                "Creative complies with policy",
                response.reason());

        assertNotNull(response.reviewedAt());

        assertEquals(
                CreativeSafetyStatus.APPROVED,
                creative.getSafetyStatus());

        verify(creativeRepository).save(creative);
    }

    @Test
    void shouldRejectPendingReview() {

        Creative creative = creative(
                101L,
                CreativeSafetyStatus.PENDING_REVIEW);

        CreativeSafetyReview review =
                review(55L, creative, SafetyReviewStatus.PENDING);

        SafetyReviewDecisionRequest request =
                new SafetyReviewDecisionRequest(
                        SafetyReviewStatus.REJECTED,
                        "Reviewer",
                        "Policy violation");

        when(reviewRepository.findById(55L))
                .thenReturn(Optional.of(review));

        when(reviewRepository.save(any(CreativeSafetyReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreativeSafetyReviewResponse response =
                reviewService.decideReview(55L, request);

        assertEquals(SafetyReviewStatus.REJECTED, response.status());

        assertEquals(
                CreativeSafetyStatus.REJECTED,
                creative.getSafetyStatus());
    }

    @Test
    void shouldRejectPendingAsFinalDecision() {

        SafetyReviewDecisionRequest request =
                new SafetyReviewDecisionRequest(
                        SafetyReviewStatus.PENDING,
                        "Reviewer",
                        "Still reviewing");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> reviewService.decideReview(55L, request));

        assertEquals(
                "Review decision must be APPROVED or REJECTED",
                exception.getMessage());

        verify(reviewRepository, never()).findById(55L);
    }

    @Test
    void shouldThrowWhenSafetyReviewDoesNotExist() {

        SafetyReviewDecisionRequest request =
                new SafetyReviewDecisionRequest(
                        SafetyReviewStatus.APPROVED,
                        "Reviewer",
                        "Approved");

        when(reviewRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> reviewService.decideReview(999L, request));

        assertEquals(
                "Safety review not found: 999",
                exception.getMessage());
    }

    @Test
    void shouldRejectAlreadyCompletedReview() {

        Creative creative = creative(
                101L,
                CreativeSafetyStatus.APPROVED);

        CreativeSafetyReview review =
                review(55L, creative, SafetyReviewStatus.APPROVED);

        SafetyReviewDecisionRequest request =
                new SafetyReviewDecisionRequest(
                        SafetyReviewStatus.REJECTED,
                        "Reviewer",
                        "Changed decision");

        when(reviewRepository.findById(55L))
                .thenReturn(Optional.of(review));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> reviewService.decideReview(55L, request));

        assertEquals(
                "Safety review is already completed: 55",
                exception.getMessage());

        verify(creativeRepository, never())
                .save(any(Creative.class));
    }

    @Test
    void shouldReturnReviewHistoryForCreative() {

        Creative creative = creative(
                101L,
                CreativeSafetyStatus.APPROVED);

        CreativeSafetyReview first =
                review(56L, creative, SafetyReviewStatus.APPROVED);

        CreativeSafetyReview second =
                review(55L, creative, SafetyReviewStatus.REJECTED);

        when(creativeRepository.existsById(101L))
                .thenReturn(true);

        when(reviewRepository.findByCreative_IdOrderByCreatedAtDesc(101L))
                .thenReturn(List.of(first, second));

        List<CreativeSafetyReviewResponse> responses =
                reviewService.getReviewsForCreative(101L);

        assertEquals(2, responses.size());
        assertEquals(56L, responses.get(0).id());
        assertEquals(55L, responses.get(1).id());
    }

    @Test
    void shouldRejectReviewHistoryForMissingCreative() {

        when(creativeRepository.existsById(999L))
                .thenReturn(false);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> reviewService.getReviewsForCreative(999L));

        assertEquals(
                "Creative not found: 999",
                exception.getMessage());

        verify(reviewRepository, never())
                .findByCreative_IdOrderByCreatedAtDesc(999L);
    }

    private Creative creative(
            Long id,
            CreativeSafetyStatus safetyStatus) {

        return Creative.builder()
                .id(id)
                .name("Sports Pre-Roll")
                .safetyStatus(safetyStatus)
                .build();
    }

    private CreativeSafetyReview review(
            Long id,
            Creative creative,
            SafetyReviewStatus status) {

        return CreativeSafetyReview.builder()
                .id(id)
                .creative(creative)
                .status(status)
                .reviewSource(SafetyReviewSource.HUMAN)
                .build();
    }
}