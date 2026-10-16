package com.adsplatform.serving.decision;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdDecisionSlotRepository extends JpaRepository<AdDecisionSlot, Long> {

    // The click token is unique, so at most one slot matches.
    Optional<AdDecisionSlot> findByClickToken(String clickToken);
}