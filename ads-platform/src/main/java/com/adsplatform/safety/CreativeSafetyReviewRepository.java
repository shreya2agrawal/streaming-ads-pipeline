package com.adsplatform.safety;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreativeSafetyReviewRepository
        extends JpaRepository<CreativeSafetyReview, Long> {

    List<CreativeSafetyReview> findByCreative_IdOrderByCreatedAtDesc(
            Long creativeId);
}