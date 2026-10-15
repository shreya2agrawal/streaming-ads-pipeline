package com.adsplatform.campaign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.adsplatform.placement.PlacementRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final PlacementRepository placementRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    public Campaign getCampaignById(Long id) {
        return campaignRepository.findById(id).orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
    }

    public Campaign createCampaign(Campaign campaign) {
        campaign.setStatus(Campaign.CampaignStatus.DRAFT);
        campaign.setImpressionsDelivered(0L);
        campaign.setPacingPercent(0.0);
        campaign.setSpent(BigDecimal.ZERO);
        validateBid(campaign.getBidCpc());
        
        campaign.setTargetShowIds(
                campaign.getTargetShowIds() == null
                        ? new HashSet<>()
                        : new HashSet<>(campaign.getTargetShowIds()));

        campaign.setTargetingSegments(
                campaign.getTargetingSegments() == null
                        ? new HashSet<>()
                        : new HashSet<>(campaign.getTargetingSegments()));

        campaign.setTargetPlacementIds(
                campaign.getTargetPlacementIds() == null
                        ? new HashSet<>()
                        : new HashSet<>(campaign.getTargetPlacementIds()));

        campaign.setTargetDeviceTypes(
                campaign.getTargetDeviceTypes() == null
                        ? new HashSet<>()
                        : new HashSet<>(campaign.getTargetDeviceTypes()));

        validateTargetPlacements(campaign);

        Campaign saved = campaignRepository.save(campaign);
        kafkaTemplate.send("campaign-events", "CREATED: " + saved.getId());
        log.info("Campaign Created: {}", saved);
        return saved;
    }

    public Campaign updateCampaign(Long id, Campaign updated) {
        Campaign existing = getCampaignById(id);
        existing.setName(updated.getName());
        existing.setBudget(updated.getBudget());
        existing.setStatus(updated.getStatus());
        existing.setPacingPercent(updated.getPacingPercent());
        existing.setImpressionsDelivered(updated.getImpressionsDelivered());
        existing.setAdvertiser(updated.getAdvertiser());
        existing.setAgency(updated.getAgency());
        existing.setFlightStart(updated.getFlightStart());
        existing.setFlightEnd(updated.getFlightEnd());
        existing.setImpressionsGoal(updated.getImpressionsGoal());

        if (updated.getBidCpc() != null) {
            validateBid(updated.getBidCpc());
            existing.setBidCpc(updated.getBidCpc());
        }

        existing.setTargetShowIds(
                updated.getTargetShowIds() == null
                        ? new HashSet<>()
                        : new HashSet<>(updated.getTargetShowIds()));

        existing.setTargetingSegments(
                updated.getTargetingSegments() == null
                        ? new HashSet<>()
                        : new HashSet<>(updated.getTargetingSegments()));

        existing.setTargetPlacementIds(
                updated.getTargetPlacementIds() == null
                        ? new HashSet<>()
                        : new HashSet<>(updated.getTargetPlacementIds()));

        existing.setTargetDeviceTypes(
                updated.getTargetDeviceTypes() == null
                        ? new HashSet<>()
                        : new HashSet<>(updated.getTargetDeviceTypes()));

        validateTargetPlacements(existing);

        Campaign saved = campaignRepository.save(existing);
        kafkaTemplate.send("campaign-events", "UPDATED: " + saved.getId());
        return saved;
    }

    public void deleteCampaign(Long id) {
        campaignRepository.deleteById(id);
    }

    public List<Campaign> getUnderPacingCampaigns(double threshold) {
        return campaignRepository.findUnderPacing(threshold);
    }

    public long countActive() {
        return campaignRepository.countByStatus(Campaign.CampaignStatus.ACTIVE);
    }

    private void validateTargetPlacements(Campaign campaign) {
        for (Long placementId : campaign.getTargetPlacementIds()) {
            if (!placementRepository.existsById(placementId)) {
                throw new IllegalArgumentException(
                        "Target placement not found: " + placementId);
            }
        }
    }

    private void validateBid(BigDecimal bidCpc) {
        if (bidCpc != null && bidCpc.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Bid per click must be greater than zero");
        }
    }
}
