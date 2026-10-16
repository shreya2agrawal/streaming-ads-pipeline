package com.adsplatform.serving.decision;

import java.util.List;

/*
 * Result of one ad decision.
 *
 * winners:        ads chosen for the slots, best first
 * exclusions:     candidates the rules rejected, with reasons
 * candidateCount: how many candidates were considered
 * noFillReason:   set only when no ad was chosen
 */
public record DecisionOutcome(
    List<RankedAd> winners,
    List<Exclusion> exclusions,
    int candidateCount,
    NoFillReason noFillReason
) {

    public enum NoFillReason {
        NO_CANDIDATES,
        NO_ELIGIBLE_CANDIDATES
    }

    public DecisionOutcome {
        winners = List.copyOf(winners);
        exclusions = List.copyOf(exclusions);
    }

    public boolean filled() {
        return !winners.isEmpty();
    }

}
