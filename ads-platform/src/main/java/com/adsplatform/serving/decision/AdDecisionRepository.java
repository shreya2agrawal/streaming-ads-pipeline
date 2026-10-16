package com.adsplatform.serving.decision;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdDecisionRepository extends JpaRepository<AdDecision, UUID> {
}