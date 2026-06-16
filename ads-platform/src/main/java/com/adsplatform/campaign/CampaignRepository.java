package com.adsplatform.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByStatus(Campaign.CampaignStatus status);
    List<Campaign> findByAdvertiserContainingIgnoreCase(String adviser);
    @Query("SELECT c FROM Campaign c WHERE c.pacingPercent < :threshold AND c.status = 'ACTIVE'")
    List<Campaign> findUnderPacing(@Param("threshold") double threshold);
    long countByStatus(Campaign.CampaignStatus status);
}