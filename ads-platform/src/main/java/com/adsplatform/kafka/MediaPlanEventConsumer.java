package com.adsplatform.kafka;

import com.adsplatform.mediaplan.MediaPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaPlanEventConsumer {

    private final MediaPlanRepository mediaPlanRepository;

    @KafkaListener(
            topics = "mediaplan-events",
            groupId = "ads-platform-group"
    )
    public void handleMediaPlanEvent(String message) {
        log.info("Received media plan event: {}", message);

        try {
            String[] parts = message.split(":");
            String eventType = parts[0].trim();
            Long planId = Long.parseLong(parts[1].trim());

            switch (eventType) {
                case "CREATED"              -> handlePlanCreated(planId);
                case "PROPOSAL_SENT"        -> handleProposalSent(planId);
                case "APPROVED"             -> handlePlanApproved(planId);
                case "INVENTORY_ALLOCATED"  -> handleInventoryAllocated(planId, parts);
                case "ACTIVATED"            -> handlePlanActivated(planId);
                case "COMPLETED"            -> handlePlanCompleted(planId);
                default -> log.warn("Unknown media plan event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process mediaplan event: {} — {}", message, e.getMessage());
        }
    }

    private void handlePlanCreated(Long planId) {
        mediaPlanRepository.findById(planId).ifPresent(plan ->
                log.info("New media plan created: {} for {} ({})",
                        plan.getName(), plan.getAdvertiser(), plan.getPlanType())
        );
    }

    private void handleProposalSent(Long planId) {
        mediaPlanRepository.findById(planId).ifPresent(plan -> {
            log.info("Proposal sent for: {} — budget: ${}",
                    plan.getName(), plan.getTotalBudget());
            // In production: send email to agency contact,
            // log to CRM system
        });
    }

    private void handlePlanApproved(Long planId) {
        mediaPlanRepository.findById(planId).ifPresent(plan -> {
            log.info("Plan approved: {} — ready for inventory allocation",
                    plan.getName());
            // In production: notify ad ops team to start
            // inventory allocation process
        });
    }

    private void handleInventoryAllocated(Long planId, String[] parts) {
        long slotsAllocated = parts.length > 2 ? Long.parseLong(parts[2].trim()) : 0;
        mediaPlanRepository.findById(planId).ifPresent(plan ->
                log.info("Inventory allocated for: {} — {} slots across {} shows",
                        plan.getName(), slotsAllocated,
                        plan.getTargetShowIds() != null ? plan.getTargetShowIds().size() : 0)
        );
    }

    private void handlePlanActivated(Long planId) {
        mediaPlanRepository.findById(planId).ifPresent(plan -> {
            log.info("Plan ACTIVATED: {} — flight: {} to {}",
                    plan.getName(), plan.getFlightStart(), plan.getFlightEnd());
            // In production: trigger ad serving setup,
            // initialize pacing algorithm, start delivery tracking
        });
    }

    private void handlePlanCompleted(Long planId) {
        mediaPlanRepository.findById(planId).ifPresent(plan -> {
            log.info("Plan COMPLETED: {} — final completion rate: {}%",
                    plan.getName(), plan.getCompletionRate());
            // In production: generate completion report,
            // trigger billing reconciliation,
            // send post-campaign report to advertiser
        });
    }
}