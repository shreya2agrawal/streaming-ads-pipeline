package com.adsplatform.mediaplan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "media_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String advertiser;

    @Column(nullable = false)
    private String agency;

    @Column(nullable = false)
    private BigDecimal totalBudget;

    @Column(nullable = false)
    private LocalDate flightStart;

    @Column(nullable = false)
    private LocalDate flightEnd;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    @Column(nullable = false)
    private Long totalSlotsRequested;

    private Long totalSlotsAllocated;

    private Double completionRate;

    @ElementCollection
    @CollectionTable(
            name = "media_plan_show_ids",
            joinColumns = @JoinColumn(name = "media_plan_id")
    )
    @Column(name = "show_id")
    private List<String> targetShowIds;

    @ElementCollection
    @CollectionTable(
            name = "media_plan_targeting",
            joinColumns = @JoinColumn(name = "media_plan_id")
    )
    @Column(name = "segment")
    private List<String> targetingSegments;

    private String rfpReference;

    private String notes;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime activatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if(status == null) status = PlanStatus.DRAFT;
        if(totalSlotsAllocated == null) totalSlotsAllocated = 0L;
        if(completionRate == null) completionRate = 0.0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        recalculateCompletionRate();
    }

    private void recalculateCompletionRate() {
        if (totalSlotsRequested == null || totalSlotsRequested == 0) {
            completionRate = 0.0;
            return;
        }
        completionRate = (double) totalSlotsAllocated / totalSlotsRequested * 100;
    }

    public boolean isFullyAllocated() {
        return totalSlotsAllocated >= totalSlotsRequested;
    }

    public Long getRemainingSlots(){
        return totalSlotsRequested - totalSlotsAllocated;
    }

    public enum PlanStatus {
        DRAFT,          // just created
        PROPOSAL_SENT,  // RFP response sent to agency
        NEGOTIATING,    // back and forth with agency
        APPROVED,       // agency approved the plan
        ACTIVE,         // campaign is live
        COMPLETED,      // flight ended
        CANCELLED       // cancelled before activation
    }

    public enum PlanType {
        UPFRONT,        // reserved in advance, guaranteed delivery
        PROGRAMMATIC,   // real-time bidding
        DIRECT,         // direct deal with advertiser
        RFP             // response to a request for proposal
    }

}
