package com.adsplatform.serving.click;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdClickRepository extends JpaRepository<AdClick, Long> {

    /*
     * Claims the click. Returns 1 for the first click on a token and
     * 0 for any repeat, including a repeat arriving at the same moment:
     * the unique constraint lets only one insert win.
     * The check and the claim are one statement, so there is no gap
     * between them.
     */
    @Modifying
    @Query(value = """
            INSERT INTO ad_clicks
                (click_token, decision_id, campaign_id, creative_id, charged_amount, clicked_at)
            VALUES
                (:clickToken, :decisionId, :campaignId, :creativeId, 0, :clickedAt)
            ON CONFLICT (click_token) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("clickToken") String clickToken,
            @Param("decisionId") UUID decisionId,
            @Param("campaignId") Long campaignId,
            @Param("creativeId") Long creativeId,
            @Param("clickedAt") Instant clickedAt);

    @Modifying
    @Query("UPDATE AdClick click SET click.chargedAmount = :amount WHERE click.clickToken = :clickToken")
    int setChargedAmount(
            @Param("clickToken") String clickToken,
            @Param("amount") BigDecimal amount);

    long countByClickToken(String clickToken);

    // The click token is unique, so at most one click matches.
    Optional<AdClick> findByClickToken(String clickToken);
}