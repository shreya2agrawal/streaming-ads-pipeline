package com.adsplatform.serving.impression;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.serving.decision.AdDecisionSlot;
import com.adsplatform.serving.decision.AdDecisionSlotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * Records that a served ad was actually shown, once per slot.
 *
 * Nothing is charged here: billing is per click. Impressions are
 * measured so CTR and advertiser stats can be calculated.
 */

@Slf4j 
@Service 
@RequiredArgsConstructor 
public class ImpressionService {
    
    static final Duration IMPRESSION_TOKEN_TTL = Duration.ofHours(24);

    public enum ImpressionOutcome {
        RECORDED,   // first report for this slot
        DUPLICATE,  // already recorded (retries, double reports)
        EXPIRED     // slot too old to count
    }

    private final AdDecisionSlotRepository slotRepository;
    private final AdImpressionRepository impressionRepository;
    private final Clock clock;

    @Transactional 
    public ImpressionOutcome recordImpression(String slotToken){
        
        AdDecisionSlot slot = slotRepository.findByClickToken(slotToken)
        .orElseThrow(() -> new ResourceNotFoundException("Unknown impression"));

        Instant now = clock.instant();

        if(slot.getCreatedAt().plus(IMPRESSION_TOKEN_TTL).isBefore(now)) {
            log.info("Expired impression for slot {}: not recorded", slot.getId());
            return ImpressionOutcome.EXPIRED;
        }

        int inserted = impressionRepository.insertIfAbsent(
            slotToken, 
            slot.getDecision().getDecisionId(), 
            slot.getCampaignId(), 
            slot.getCreativeId(), 
            now);

        return inserted == 1 ? ImpressionOutcome.RECORDED : ImpressionOutcome.DUPLICATE;
    }
}
