package com.adsplatform.serving.decision;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.adsplatform.placement.DeviceType;
import com.adsplatform.serving.decision.DecisionOutcome.NoFillReason;
import com.adsplatform.serving.model.AdRequestContext;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * The record of one ad decision, kept so clicks can be traced back
 * to the exact slot, campaign and price, and so fill rate can be
 * measured. No-fills are recorded too.
 */
@Entity
@Table(name = "ad_decisions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdDecision {

    @Id
    @Column(name = "decision_id")
    private UUID decisionId;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "placement_id", nullable = false)
    private Long placementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Column(name = "show_id", nullable = false)
    private String showId;

    @Column(name = "viewer_track_id")
    private String viewerTrackId;

    @Column(name = "requested_slots", nullable = false)
    private int requestedSlots;

    @Column(name = "filled_slots", nullable = false)
    private int filledSlots;

    @Column(name = "candidate_count", nullable = false)
    private int candidateCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "no_fill_reason")
    private NoFillReason noFillReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "decision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("slotPosition")
    private List<AdDecisionSlot> slots = new ArrayList<>();

    public static AdDecision start(
            UUID decisionId,
            AdRequestContext context,
            DecisionOutcome outcome,
            Instant createdAt) {

        AdDecision decision = new AdDecision();
        decision.decisionId = decisionId;
        decision.requestId = context.requestId();
        decision.placementId = context.placementId();
        decision.deviceType = context.deviceType();
        decision.showId = context.showId();
        decision.viewerTrackId = context.viewerTrackId();
        decision.requestedSlots = context.availableSlots();
        decision.candidateCount = outcome.candidateCount();
        decision.noFillReason = outcome.noFillReason();
        decision.filledSlots = 0;
        decision.createdAt = createdAt;
        return decision;
    }

    /*
     * Slots are numbered 1, 2, 3 in the order they are added,
     * which is the ranking order.
     */
    public void addSlot(RankedAd ad, String clickToken) {
        int position = slots.size() + 1;

        if (position > requestedSlots) {
            throw new IllegalStateException(
                    "Cannot fill more slots than were requested");
        }

        slots.add(new AdDecisionSlot(this, position, ad, clickToken, createdAt));
        filledSlots = slots.size();
    }

    public List<AdDecisionSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }
}