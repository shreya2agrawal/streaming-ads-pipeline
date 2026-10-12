package com.adsplatform.creative;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CreativeRepository extends JpaRepository<Creative, Long>{

    List<Creative> findByCampaign_Id(Long campaign_id);
}
