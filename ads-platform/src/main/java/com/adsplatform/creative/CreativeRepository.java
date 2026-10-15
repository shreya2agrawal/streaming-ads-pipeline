package com.adsplatform.creative;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.adsplatform.campaign.Campaign.CampaignStatus;

public interface CreativeRepository extends JpaRepository<Creative, Long> {

    List<Creative> findByCampaign_Id(Long campaign_id);

    /*
     * Serving candidates: creatives that could possibly serve.
     *
     * Filters only what is cheap and certain. Targeting is left to
     * the eligibility rules, so exclusions can be explained.
     * JOIN FETCH loads each creative's campaign in the same query.
     */
    @Query("""
            SELECT creative
            FROM Creative creative
            JOIN FETCH creative.campaign campaign
            WHERE campaign.status = :campaignStatus
            AND campaign.bidCpc IS NOT NULL
            AND campaign.budget - campaign.spent >= campaign.bidCpc
            AND campaign.flightStart <= :latestDate
            AND campaign.flightEnd >= :earliestDate
            AND creative.status = :creativeStatus
            AND creative.safetyStatus = :safetyStatus
            AND creative.creativeType IN :creativeTypes
            """)
    List<Creative> findServableCreatives(
            @Param("campaignStatus") CampaignStatus campaignStatus,
            @Param("creativeStatus") CreativeStatus creativeStatus,
            @Param("safetyStatus") CreativeSafetyStatus safetyStatus,
            @Param("earliestDate") LocalDate earliestDate,
            @Param("latestDate") LocalDate latestDate,
            @Param("creativeTypes") Collection<CreativeType> creativeTypes);
}
