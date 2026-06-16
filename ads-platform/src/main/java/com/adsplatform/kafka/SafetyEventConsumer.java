package com.adsplatform.kafka;

import com.adsplatform.safety.SafetyFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SafetyEventConsumer {

    private final SafetyFlagRepository flagRepository;

    @KafkaListener(
            topics = "safety-flags",
            groupId = "ads-platform-group"
    )
    public void handleSafetyEvent(String message){
        log.info("Received safety event: {}", message);
        try{
            String[] parts = message.split(":");
            String eventType = parts[0].trim();
            Long flagId = Long.parseLong(parts[1].trim());
            String severity = parts.length > 2 ? parts[2].trim() : "UNKNOWN";
            if("FLAG".equals(eventType)){
                handleNewFlag(flagId, severity);
            } else {
                log.warn("Unknown safety event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process safety event: {} — {}", message, e.getMessage());
        }
    }

    private void handleNewFlag(Long flagId, String severity){
        flagRepository.findById(flagId).ifPresentOrElse(
                flag -> {
                    log.warn("Safety flag raised — id: {}, creative: {}, severity: {}",
                            flag.getId(), flag.getCreativeFile(), flag.getSeverity());

                    // Route based on severity
                    switch (flag.getSeverity()){
                        case POLICY_VIOLATION -> {
                            log.error("CRITICAL: Policy violation detected for advertiser: {}" +
                                            " — creative blocked immediately: {}",
                                    flag.getAdvertiser(), flag.getCreativeFile());
                            // In production: immediately block ad serving,
                            // notify legal team, suspend advertiser account
                        }
                        case TIER_3 -> {
                            log.warn("TIER 3 flag for advertiser: {} — routing to human review",
                                    flag.getAdvertiser());
                            // In production: create Jira ticket,
                            // notify review team via Slack
                        }
                        case ADVISORY -> {
                            log.info("Advisory flag for advertiser: {} — logged for review",
                                    flag.getAdvertiser());
                            // In production: add to weekly review queue
                        }
                    }
                },
                () -> log.warn("Safety flag not found for id: {}", flagId)
        );
    }

}
