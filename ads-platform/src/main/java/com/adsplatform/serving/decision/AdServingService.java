package com.adsplatform.serving.decision;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.placement.PlacementRepository;
import com.adsplatform.serving.decision.dto.AdDecisionRequest;
import com.adsplatform.serving.decision.dto.AdDecisionResponse;
import com.adsplatform.serving.decision.dto.AdDecisionResponse.Explanation;
import com.adsplatform.serving.decision.dto.AdDecisionResponse.ScoredAd;
import com.adsplatform.serving.decision.dto.AdDecisionResponse.ServedAd;
import com.adsplatform.serving.model.AdRequestContext;

import lombok.RequiredArgsConstructor;

/*
 * Serves one ad request end to end:
 * decide, record the decision with click tokens, build the response.
 *
 * AdDecisionService decides; this class records and presents.
 */
@Service
@RequiredArgsConstructor
public class AdServingService {

    static final String CLICK_PATH = "/api/v1/clicks/";
    static final String IMPRESSION_PATH = "/api/v1/impressions/";

    /*
     * Click tokens must be unguessable: anyone who could predict one
     * could fake clicks and charge advertisers. 32 random bytes from
     * SecureRandom, as 43 URL-safe characters.
     */
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final AdDecisionService adDecisionService;
    private final AdDecisionRepository adDecisionRepository;
    private final CreativeRepository creativeRepository;
    private final PlacementRepository placementRepository;

    @Transactional
    public AdDecisionResponse serve(AdDecisionRequest request, boolean debug) {

        // Asking for a slot that does not exist is a caller mistake, not a no-fill.
        if (!placementRepository.existsById(request.placementId())) {
            throw new ResourceNotFoundException(
                    "Placement not found: " + request.placementId());
        }

        Instant now = Instant.now();
        AdRequestContext context = request.toContext(now);
        DecisionOutcome outcome = adDecisionService.decide(context);

        AdDecision decision = AdDecision.start(UUID.randomUUID(), context, outcome, now);

        for (RankedAd winner : outcome.winners()) {
            decision.addSlot(winner, newClickToken());
        }

        adDecisionRepository.save(decision);

        return toResponse(decision, outcome, debug);
    }

    private AdDecisionResponse toResponse(
            AdDecision decision,
            DecisionOutcome outcome,
            boolean debug) {

        // The player needs each winner's media; one query for all of them.
        List<Long> creativeIds = decision.getSlots().stream()
                .map(AdDecisionSlot::getCreativeId)
                .toList();

        Map<Long, Creative> creatives = creativeRepository.findAllById(creativeIds).stream()
                .collect(Collectors.toMap(Creative::getId, Function.identity()));

        List<ServedAd> ads = decision.getSlots().stream()
                .map(slot -> {
                    Creative creative = creatives.get(slot.getCreativeId());

                    if (creative == null) {
                        throw new IllegalStateException(
                                "Winning creative not found: " + slot.getCreativeId());
                    }

                    return new ServedAd(
                            slot.getSlotPosition(),
                            slot.getCampaignId(),
                            slot.getCreativeId(),
                            creative.getCreativeType(),
                            creative.getMediaUrl(),
                            creative.getDurationSeconds(),
                            CLICK_PATH + slot.getClickToken(),
                            IMPRESSION_PATH + slot.getClickToken());
                })
                .toList();

        Explanation explanation = debug
                ? new Explanation(
                        outcome.candidateCount(),
                        decision.getSlots().stream()
                                .map(slot -> new ScoredAd(
                                        slot.getSlotPosition(),
                                        slot.getCampaignId(),
                                        slot.getCreativeId(),
                                        slot.getPriceCpc(),
                                        slot.getPredictedCtr(),
                                        slot.getRankingScore()))
                                .toList(),
                        outcome.exclusions())
                : null;

        return new AdDecisionResponse(
                decision.getDecisionId(),
                ads,
                outcome.noFillReason() == null ? null : outcome.noFillReason().name(),
                explanation);
    }

    private static String newClickToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return TOKEN_ENCODER.encodeToString(bytes);
    }
}