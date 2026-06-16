package com.adsplatform.mediaplan;

import com.adsplatform.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaPlanService {

    private final MediaPlanRepository mediaPlanRepository;
    private final InventoryService inventoryService;
    private final KafkaTemplate<String, String> kafkaTemplate;

//    BASIC CRUD ------------------------------------------------------------------------------

    public List<MediaPlan> getAllPlans() {
        return mediaPlanRepository.findAll();
    }

    public MediaPlan getPlanById(Long id) {
        return mediaPlanRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Media plan not found for id: " + id)
        );
    }

    public MediaPlan createPlan(MediaPlan plan) {
        plan.setStatus(MediaPlan.PlanStatus.DRAFT);
        plan.setTotalSlotsAllocated(0L);
        MediaPlan saved = mediaPlanRepository.save(plan);
        kafkaTemplate.send("mediaplan-events", "CREATED: " + saved.getId());
        log.info("Media plan created: {} for {}", saved.getName(), saved.getAdvertiser());
        return saved;
    }

    public MediaPlan updatePlan(Long id, MediaPlan updated) {
        MediaPlan existing = getPlanById(id);
        existing.setName(updated.getName());
        existing.setTotalBudget(updated.getTotalBudget());
        existing.setFlightStart(updated.getFlightStart());
        existing.setFlightEnd(updated.getFlightEnd());
        existing.setNotes(updated.getNotes());
        existing.setTargetingSegments(updated.getTargetingSegments());
        return mediaPlanRepository.save(existing);
    }

//    Workflow: RFP → Proposal → Negotiation → Approval --------------------------

    public MediaPlan sendProposal(Long id) {
        MediaPlan plan = getPlanById(id);
        validateStatusTransition(plan, MediaPlan.PlanStatus.DRAFT, MediaPlan.PlanStatus.PROPOSAL_SENT);
        plan.setStatus(MediaPlan.PlanStatus.PROPOSAL_SENT);
        MediaPlan saved = mediaPlanRepository.save(plan);
        kafkaTemplate.send("mediaplan-events", "PROPOSAL_SENT: " + saved.getId());
        log.info("Proposal sent for plan: {}", saved.getName());
        return saved;
    }

    public MediaPlan startNegotiation(Long id) {
        MediaPlan plan = getPlanById(id);
        validateStatusTransition(plan, MediaPlan.PlanStatus.PROPOSAL_SENT,
                MediaPlan.PlanStatus.NEGOTIATING);
        plan.setStatus(MediaPlan.PlanStatus.NEGOTIATING);
        return mediaPlanRepository.save(plan);
    }

    public MediaPlan approvePlan(Long id) {
        MediaPlan plan = getPlanById(id);
        if (plan.getStatus() != MediaPlan.PlanStatus.PROPOSAL_SENT &&
                plan.getStatus() != MediaPlan.PlanStatus.NEGOTIATING) {
            throw new RuntimeException("Plan can only be approved from PROPOSAL_SENT or NEGOTIATING status");
        }
        plan.setStatus(MediaPlan.PlanStatus.APPROVED);
        MediaPlan saved = mediaPlanRepository.save(plan);
        kafkaTemplate.send("mediaplan-events", "APPROVED:" + saved.getId());
        log.info("Plan approved: {}", saved.getName());
        return saved;
    }

//    INVENTORY ALLOCATION ------------------------------------------------------------------------------

    @Transactional
    public MediaPlan allocateInventory(Long planId) {

        MediaPlan plan = getPlanById(planId);

        if (plan.getStatus() != MediaPlan.PlanStatus.APPROVED) {
            throw new RuntimeException("Inventory can only be allocated for APPROVED plans");
        }
        if (plan.getTargetShowIds() == null || plan.getTargetShowIds().isEmpty()) {
            throw new RuntimeException("No target shows specified in the media plan");
        }
//        distribute slots evenly across target shows
        int showCount = plan.getTargetShowIds().size();
        long slotsPerShow = plan.getTotalSlotsRequested() / showCount;
        long remainder = plan.getTotalSlotsRequested() % showCount;
        long totalAllocated = 0;

        for (int i = 0; i < plan.getTargetShowIds().size(); i++) {
            String showId = plan.getTargetShowIds().get(i);
//            give the remainder slots to the first show
            long slotsForThisShow = (i == 0) ? slotsPerShow + remainder : slotsPerShow;
            try {
                inventoryService.allocateSlots(showId, slotsForThisShow);
                totalAllocated += slotsForThisShow;
                log.info("Allocated {} slots on {} for plan {}",
                        slotsForThisShow, showId, plan.getName());
            } catch (RuntimeException e) {
                log.warn("Could not allocate {} slots on {}: {}",
                        slotsForThisShow, showId, e.getMessage());
            }
        }

        plan.setTotalSlotsAllocated(totalAllocated);
        MediaPlan saved = mediaPlanRepository.save(plan);
        kafkaTemplate.send("mediaplan-events",
                "INVENTORY_ALLOCATED:" + saved.getId() + ":" + totalAllocated);
        return saved;
    }

//    Activation ---------------------------------------------------------------------------------

    @Transactional
    public MediaPlan activatePlan(Long id){
        MediaPlan plan = getPlanById(id);
        if(plan.getStatus() != MediaPlan.PlanStatus.APPROVED){
            throw new RuntimeException("Only APPROVED plans can be activated");
        }
        if(!plan.isFullyAllocated()){
            log.warn("Plan {} is only {}% allocated — activating anyway",
                    plan.getName(), plan.getCompletionRate());
        }
        plan.setStatus(MediaPlan.PlanStatus.ACTIVE);
        plan.setActivatedAt(LocalDateTime.now());
        MediaPlan saved = mediaPlanRepository.save(plan);
        kafkaTemplate.send("mediaplan-events", "ACTIVATED:" + saved.getId());
        log.info("Plan activated: {}", saved.getName());
        return saved;
    }

    public MediaPlan completePlan(Long id){
        MediaPlan plan = getPlanById(id);
        plan.setStatus(MediaPlan.PlanStatus.COMPLETED);
        MediaPlan saved = mediaPlanRepository.save(plan);
        kafkaTemplate.send("mediaplan-events", "COMPLETED:" + saved.getId());
        return saved;
    }

    public MediaPlan cancelPlan(Long id){
        MediaPlan plan = getPlanById(id);
        if (plan.getStatus() == MediaPlan.PlanStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed plan");
        }
//        release inventory back if slots were allocated
        if(plan.getTotalSlotsAllocated() > 0 && plan.getTargetShowIds() != null){
            int showCount = plan.getTargetShowIds().size();
            long slotsPerShow = plan.getTotalSlotsAllocated() / showCount;
            for(String showId : plan.getTargetShowIds()){
                try{
                    inventoryService.releaseSlots(showId, slotsPerShow);
                } catch (RuntimeException e) {
                    log.warn("Could not release slots for show {}: {}",
                            showId, e.getMessage());
                }
            }
        }
        plan.setStatus(MediaPlan.PlanStatus.CANCELLED);
        return mediaPlanRepository.save(plan);
    }

//    Analytics & Pipeline --------------------------------------------------------------------------

    public List<MediaPlan> getPipeline(){
        return mediaPlanRepository.findPipelinePlans();
    }

    public List<MediaPlan> getUnderDeliveringPlans(double threshold){
        return mediaPlanRepository.findUnderDeliveringPlans(threshold);
    }

    public MediaPlanStats getStats(){
        long draft = mediaPlanRepository.countByStatus(MediaPlan.PlanStatus.DRAFT);
        long active = mediaPlanRepository.countByStatus(MediaPlan.PlanStatus.ACTIVE);
        long approved = mediaPlanRepository.countByStatus(MediaPlan.PlanStatus.APPROVED);
        long completed = mediaPlanRepository.countByStatus(MediaPlan.PlanStatus.COMPLETED);
        List<MediaPlan> underDelivering = getUnderDeliveringPlans(80.0);
        return new MediaPlanStats(draft, active, approved, completed,underDelivering.size());
    }

//    Helper --------------------------------------------------------------------------------

    public void validateStatusTransition(MediaPlan plan,
                                         MediaPlan.PlanStatus required,
                                         MediaPlan.PlanStatus next){
        if(plan.getStatus() != required){
            throw new RuntimeException(
                    String.format("Cannot transition to %s — plan is currently %s, expected %s",
                            next, plan.getStatus(), required));
        }
    }

    public record MediaPlanStats(
            long draftCount,
            long activeCount,
            long approvedCount,
            long completedCount,
            int underDeliveringCount
    ) {}

}
