package com.adsplatform.serving.conversion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adsplatform.campaign.Campaign;
import com.adsplatform.campaign.CampaignRepository;
import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.serving.click.AdClick;
import com.adsplatform.serving.click.AdClickRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionService {

    static final Duration ATTRIBUTION_WINDOW = Duration.ofDays(30);

    static final Duration CLOCK_SKEW_ALLOWANCE = Duration.ofMinutes(5);

    public enum RecordOutcome {
        RECORDED,
        DUPLICATE,
        OUTSIDE_WINDOW
    }

    public enum CancelOutcome {
        CANCELLED,
        ALREADY_CANCELLED
    }

    private final AdClickRepository adClickRepository;
    private final AdConversionRepository adConversionRepository;
    private final CampaignRepository campaignRepository;
    private final Clock clock;

    @Transactional
    public RecordOutcome recordConversion(
            String advertiser,
            String clickToken,
            String bookingId,
            BigDecimal value,
            String currency,
            Instant convertedAt) {
        AdClick click = findOwnedClick(advertiser,clickToken);
        Instant now = clock.instant();

        if (convertedAt.isAfter(now.plus(CLOCK_SKEW_ALLOWANCE))) {
            throw new IllegalArgumentException("Conversion time is in the future");
        }

        Instant clickedAt = click.getClickedAt();
        if (convertedAt.isBefore(clickedAt)
                || convertedAt.isAfter(clickedAt.plus(ATTRIBUTION_WINDOW))) {
            log.info("Booking {} for campaign {} is outside the attribution window: not counted",
                    bookingId, click.getCampaignId());
            return RecordOutcome.OUTSIDE_WINDOW;
        }

        int inserted = adConversionRepository.insertIfAbsent(
                clickToken,
                click.getCampaignId(),
                bookingId,
                value,
                currency,
                convertedAt,
                now);

        return inserted == 1 ? RecordOutcome.RECORDED : RecordOutcome.DUPLICATE;
    }

    @Transactional
    public CancelOutcome cancelConversion(String advertiser, String clickToken, String bookingId) {
        AdClick click = findOwnedClick(advertiser, clickToken);
        Long campaignId = click.getCampaignId();

        int cancelled = adConversionRepository.cancel(
                campaignId,
                bookingId,
                clock.instant());

        if (cancelled == 1) {
            return CancelOutcome.CANCELLED;
        }

        if (adConversionRepository.findByCampaignIdAndBookingId(campaignId, bookingId).isPresent()) {
            return CancelOutcome.ALREADY_CANCELLED;
        }
        throw new ResourceNotFoundException("Unknown conversion");
    }

    private AdClick findOwnedClick(String advertiser, String clickToken) {

        AdClick click = adClickRepository.findByClickToken(clickToken)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown click"));

        boolean owned = campaignRepository.findById(click.getCampaignId())
                .map(Campaign::getAdvertiser)
                .filter(advertiser::equals)
                .isPresent();

        if (!owned) {
            throw new ResourceNotFoundException("Unknown click");
        }

        return click;
    }

}
