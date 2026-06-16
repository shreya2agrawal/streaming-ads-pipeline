package com.adsplatform.kafka;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignEventConsumer {

    private final CampaignRepository campaignRepository;

    @KafkaListener(
            topics = "campaign-events",
            groupId = "ads-platform-group"
    )
    public void handleCampaignEvent(String message){
        log.info("Received campaign event: {}", message);

        try{
            String[] parts = message.split(":");
            String eventType = parts[0].trim();
            Long campaignId = Long.parseLong(parts[1].trim());

            switch (eventType){
                case "CREATED" -> handleCampaignCreated(campaignId);
                case "UPDATED" -> handleCampaignUpdated(campaignId);
                default -> log.warn("Unknown campaign event type: {}", eventType);
            }
        }
        catch (Exception e){
            log.error("Failed to process campaign event: {} — {}", message, e.getMessage());
        }
    }

    private void handleCampaignCreated(Long campaignId){
        campaignRepository.findById(campaignId).ifPresentOrElse(
                campaign -> {
                    log.info("Processing new campaign: {} for advertiser: {}",
                            campaign.getName(), campaign.getAdvertiser());
                    // In production: trigger analytics tracking,
                    // notify sales team, initialize billing etc.
                },
                () -> log.warn("Campaign not found for id: {}", campaignId)
        );
    }

    private void handleCampaignUpdated(Long campaignId){
        campaignRepository.findById(campaignId).ifPresentOrElse(
                campaign -> {
                    log.info("Campaign updated: {} — status: {}",
                            campaign.getName(), campaign.getStatus());
                    // In production: sync to reporting systems,
                    // update billing if budget changed etc.
                    if(campaign.getStatus() == Campaign.CampaignStatus.AT_RISK){
                        log.warn("ALERT: Campaign {} is AT_RISK — pacing: {}%",
                                campaign.getName(), campaign.getPacingPercent());
                        // In production: send Slack alert to account team
                    }
                },
                () -> log.warn("Campaign not found for id: {}", campaignId)
        );
    }

}
