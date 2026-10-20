package com.adsplatform.serving.impression;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdImpressionRepository extends JpaRepository<AdImpression, Long> {

    /*
     * Records the impression. Returns 1 the first time a slot is shown,
     * and 0 for any repeat: the unique constraint lets only one insert win.
     */
    @Modifying 
    @Query(value = """
            INSERT INTO ad_impressions
            (slot_token, decision_id, campaign_id, creative_id, impressed_at)
            VALUES
            (:slotToken, :decisionId, :campaignId, :creativeId, :impressedAt)
            ON CONFLICT (slot_token) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
        @Param("slotToken") String slotToken,
        @Param("decisionId") UUID decisionId,
        @Param("campaignId") Long campaignId,
        @Param("creativeId") Long creativeId,
        @Param("impressedAt") Instant impressedAt
    );
    
    long countBySlotToken(String slotToken);
    
}
