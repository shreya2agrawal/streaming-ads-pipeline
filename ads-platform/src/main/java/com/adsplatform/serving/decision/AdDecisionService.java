package com.adsplatform.serving.decision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.adsplatform.serving.candidate.CandidateSource;
import com.adsplatform.serving.decision.DecisionOutcome.NoFillReason;
import com.adsplatform.serving.eligibility.EligibilityEngine;
import com.adsplatform.serving.eligibility.EligibilityResult;
import com.adsplatform.serving.model.AdRequestContext;
import com.adsplatform.serving.model.CandidateAd;
import com.adsplatform.serving.scoring.CtrScoringPort;

import lombok.RequiredArgsConstructor;

/*
 * Chooses the ads for one ad request:
 *
 *   candidates -> eligibility -> score -> rank
 *   -> one per campaign -> fill the available slots
 *
 * Hard rules run before scoring, so the model never scores an ad
 * that is not allowed to serve.
 */

@Service
@RequiredArgsConstructor
public class AdDecisionService {

    /*
     * Best score first. Ties go to the lower campaign id, then the
     * lower creative id, so the same input always gives the same
     * order: stable tests and replayable decisions.
     */
    private static final Comparator<RankedAd> BEST_FIRST = Comparator.comparingDouble(RankedAd::score).reversed()
            .thenComparing(RankedAd::campaignId)
            .thenComparing(RankedAd::creativeId);

    private final CandidateSource candidateSource;
    private final EligibilityEngine eligibilityEngine;
    private final CtrScoringPort ctrScoringPort;

    public DecisionOutcome decide(AdRequestContext context) {

        List<CandidateAd> candidates = candidateSource.findCandidates(context);

        if (candidates.isEmpty()) {
            return new DecisionOutcome(
                    List.of(), List.of(), 0, NoFillReason.NO_CANDIDATES);
        }

        List<CandidateAd> eligible = new ArrayList<>();
        List<Exclusion> exclusions = new ArrayList<>();

        for (CandidateAd candidate : candidates) {
            EligibilityResult result = eligibilityEngine.evaluate(candidate, context);

            if (result.eligible()) {
                eligible.add(candidate);
            } else {
                exclusions.add(new Exclusion(
                        candidate.campaign().id(),
                        candidate.creative().id(),
                        result.reasonCode(),
                        result.reason()));
            }
        }

        if (eligible.isEmpty()) {
            return new DecisionOutcome(
                    List.of(), exclusions, candidates.size(),
                    NoFillReason.NO_ELIGIBLE_CANDIDATES);
        }

        List<RankedAd> ranked = rank(eligible, context);
        List<RankedAd> winners = bestPerCampaign(ranked).stream()
                .limit(context.availableSlots())
                .toList();

        return new DecisionOutcome(winners, exclusions, candidates.size(),null);
    }

    private List<RankedAd> rank(List<CandidateAd> eligible, AdRequestContext context) {
        List<Double> predictedCtrs = ctrScoringPort.predictCtr(eligible, context);

        if (predictedCtrs == null || predictedCtrs.size() != eligible.size()) {
            throw new IllegalStateException(
                    "CTR scorer must return one prediction per candidate");
        }

        List<RankedAd> ranked = new ArrayList<>(eligible.size());

        for (int i = 0; i < eligible.size(); i++) {
            CandidateAd candidate = eligible.get(i);
            // Retrieval already excludes campaigns without a bid;
            // this keeps a missing bid from ever reaching a slot.
            if (candidate.campaign().bidCpc() == null) {
                continue;
            }
            double predictedCtr = predictedCtrs.get(i);
            double score = candidate.campaign().bidCpc().doubleValue() * predictedCtr;

            ranked.add(new RankedAd(candidate, predictedCtr, score));
        }

        ranked.sort(BEST_FIRST);
        return ranked;
    }

    /*
     * Keeps each campaign's best creative. The list is already
     * sorted best first, so the first entry per campaign wins.
     */
    private static List<RankedAd> bestPerCampaign(List<RankedAd> ranked) {
        Map<Long, RankedAd> best = new LinkedHashMap<>();

        for (RankedAd ad : ranked) {
            best.putIfAbsent(ad.campaignId(), ad);
        }

        return new ArrayList<>(best.values());
    }
}
