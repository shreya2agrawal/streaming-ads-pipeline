package com.adsplatform.serving.click;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.creative.Creative;
import com.adsplatform.creative.CreativeRepository;
import com.adsplatform.serving.decision.AdDecisionSlot;
import com.adsplatform.serving.decision.AdDecisionSlotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * Records a click and charges the campaign, exactly once per click,
 * never beyond the budget.
 *
 * Every click takes its locks in the same order (claim the click,
 * then lock the campaign), so concurrent clicks cannot deadlock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClickService {

    static final Duration CLICK_TOKEN_TTL = Duration.ofHours(24);

    private final AdDecisionSlotRepository slotRepository;
    private final AdClickRepository clickRepository;
    private final CampaignRepository campaignRepository;
    private final CreativeRepository creativeRepository;
    private final Clock clock;

    @Transactional
    public ClickResult recordClick(String clickToken) {
        AdDecisionSlot slot = slotRepository.findByClickToken(clickToken)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown click"));

        // The destination comes from the stored creative, never from the
        // request, so this endpoint cannot be used as an open redirect.
        String destinationUrl = creativeRepository.findById(slot.getCreativeId())
                .map(Creative::getDestinationUrl)
                .orElseThrow(() -> new IllegalStateException(
                        "Creative not found: " + slot.getCreativeId()));

        Instant now = clock.instant();

        if (slot.getCreatedAt().plus(CLICK_TOKEN_TTL).isBefore(now)) {
            log.info("Expired click token for slot {}: redirected, not charged", slot.getId());
            return ClickResult.expired(destinationUrl);
        }

        int inserted = clickRepository.insertIfAbsent(
                clickToken,
                slot.getDecision().getDecisionId(),
                slot.getCampaignId(),
                slot.getCreativeId(),
                now);

        if (inserted == 0) {
            return ClickResult.duplicate(destinationUrl);
        }

        BigDecimal charged = charge(slot.getCampaignId(), slot.getPriceCpc());
        clickRepository.setChargedAmount(clickToken, charged);

        return ClickResult.charged(destinationUrl, charged);
    }

    /*
     * Charges at most what is left of the budget. The row lock makes
     * read-then-write safe: a concurrent charge waits, then sees the
     * updated spend.
     */
    private BigDecimal charge(Long campaignId, BigDecimal price) {
        Campaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new IllegalStateException(
                        "Campaign not found: " + campaignId));

        BigDecimal remaining = campaign.getBudget()
                .subtract(campaign.getSpent())
                .max(BigDecimal.ZERO);

        BigDecimal charged = price.min(remaining);
        campaign.setSpent(campaign.getSpent().add(charged));

        return charged;
    }
}