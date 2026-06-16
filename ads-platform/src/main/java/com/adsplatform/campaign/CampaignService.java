package com.adsplatform.campaign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public List<Campaign> getAllCampaigns(){
        return campaignRepository.findAll();
    }

    public Campaign getCampaignById(Long id){
        return campaignRepository.findById(id).orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
    }

    public Campaign createCampaign(Campaign campaign){
        campaign.setStatus(Campaign.CampaignStatus.DRAFT);
        campaign.setImpressionsDelivered(0L);
        campaign.setPacingPercent(0.0);
        Campaign saved = campaignRepository.save(campaign);
        kafkaTemplate.send("campaign-events", "CREATED: " + saved.getId());
        log.info("Campaign Created: {}" + saved);
        return saved;
    }

    public Campaign updateCampaign(Long id, Campaign updated){
        Campaign existing = getCampaignById(id);
        existing.setName(updated.getName());
        existing.setBudget(updated.getBudget());
        existing.setStatus(updated.getStatus());
        existing.setPacingPercent(updated.getPacingPercent());
        existing.setImpressionsDelivered(updated.getImpressionsDelivered());
        Campaign saved = campaignRepository.save(existing);
        kafkaTemplate.send("campaign-events", "UPDATED: " + saved.getId());
        return saved;
    }

    public void deleteCampaign(Long id){
        campaignRepository.deleteById(id);
    }

    public List<Campaign> getUnderPacingCampaigns(double threshold){
        return campaignRepository.findUnderPacing(threshold);
    }

    public long countActive(){
        return campaignRepository.countByStatus(Campaign.CampaignStatus.ACTIVE);
    }
}
