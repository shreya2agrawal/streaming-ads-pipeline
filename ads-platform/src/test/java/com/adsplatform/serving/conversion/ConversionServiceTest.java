package com.adsplatform.serving.conversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.adsplatform.common.exception.ResourceNotFoundException;
import com.adsplatform.serving.click.AdClick;
import com.adsplatform.serving.click.AdClickRepository;
import com.adsplatform.serving.conversion.ConversionService.CancelOutcome;
import com.adsplatform.serving.conversion.ConversionService.RecordOutcome;

class ConversionServiceTest {

    private static final String TOKEN = "token-abc";
    private static final String BOOKING = "HR-1001";
    private static final BigDecimal VALUE = new BigDecimal("2400.00");
    private static final Instant NOW = Instant.parse("2026-10-21T15:00:00Z");

    private AdClickRepository clickRepository;
    private AdConversionRepository conversionRepository;
    private ConversionService conversionService;

    @BeforeEach
    void setUp() {
        clickRepository = mock(AdClickRepository.class);
        conversionRepository = mock(AdConversionRepository.class);
        conversionService = new ConversionService(
                clickRepository,
                conversionRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // A click on campaign 10, made `ago` before NOW.
    private Instant clickMade(Duration ago) {
        Instant clickedAt = NOW.minus(ago);
        AdClick click = mock(AdClick.class);
        when(click.getCampaignId()).thenReturn(10L);
        when(click.getClickedAt()).thenReturn(clickedAt);
        when(clickRepository.findByClickToken(TOKEN)).thenReturn(Optional.of(click));
        return clickedAt;
    }

    private RecordOutcome record(Instant convertedAt) {
        return conversionService.recordConversion(TOKEN, BOOKING, VALUE, "USD", convertedAt);
    }

    // Recording

    @Test
    void unknownClickIsNotFound() {
        when(clickRepository.findByClickToken("made-up")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversionService.recordConversion(
                "made-up", BOOKING, VALUE, "USD", NOW))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(conversionRepository, never())
                .insertIfAbsent(anyString(), anyLong(), anyString(), any(), anyString(), any(), any());
    }

    @Test
    void firstReportIsRecordedForTheClicksCampaign() {
        Instant clickedAt = clickMade(Duration.ofHours(2));
        Instant bookedAt = clickedAt.plus(Duration.ofMinutes(30));
        when(conversionRepository.insertIfAbsent(
                eq(TOKEN), eq(10L), eq(BOOKING), any(), any(), any(), any())).thenReturn(1);

        assertThat(record(bookedAt)).isEqualTo(RecordOutcome.RECORDED);

        // Credited to the click's campaign; received now, booked earlier.
        verify(conversionRepository).insertIfAbsent(
                TOKEN, 10L, BOOKING, VALUE, "USD", bookedAt, NOW);
    }

    @Test
    void repeatedReportIsADuplicate() {
        clickMade(Duration.ofHours(2));
        when(conversionRepository.insertIfAbsent(
                eq(TOKEN), eq(10L), eq(BOOKING), any(), any(), any(), any())).thenReturn(0);

        assertThat(record(NOW.minus(Duration.ofHours(1)))).isEqualTo(RecordOutcome.DUPLICATE);
    }

    @Test
    void bookingBeforeTheClickIsOutsideTheWindow() {
        Instant clickedAt = clickMade(Duration.ofHours(2));

        assertThat(record(clickedAt.minus(Duration.ofMinutes(1))))
                .isEqualTo(RecordOutcome.OUTSIDE_WINDOW);

        verify(conversionRepository, never())
                .insertIfAbsent(anyString(), anyLong(), anyString(), any(), anyString(), any(), any());
    }

    @Test
    void bookingMoreThan30DaysAfterTheClickIsOutsideTheWindow() {
        Instant clickedAt = clickMade(Duration.ofDays(40));

        assertThat(record(clickedAt.plus(Duration.ofDays(31))))
                .isEqualTo(RecordOutcome.OUTSIDE_WINDOW);
    }

    @Test
    void bookingJustInsideTheWindowIsRecorded() {
        Instant clickedAt = clickMade(Duration.ofDays(40));
        when(conversionRepository.insertIfAbsent(
                eq(TOKEN), eq(10L), eq(BOOKING), any(), any(), any(), any())).thenReturn(1);

        assertThat(record(clickedAt.plus(Duration.ofDays(29))))
                .isEqualTo(RecordOutcome.RECORDED);
    }

    @Test
    void bookingInTheFutureIsRejected() {
        clickMade(Duration.ofHours(2));

        assertThatThrownBy(() -> record(NOW.plus(Duration.ofHours(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Conversion time is in the future");
    }

    @Test
    void smallClockDifferenceIsAllowed() {
        clickMade(Duration.ofHours(2));
        when(conversionRepository.insertIfAbsent(
                eq(TOKEN), eq(10L), eq(BOOKING), any(), any(), any(), any())).thenReturn(1);

        // Two minutes "in the future" is clock drift, not a mistake.
        assertThat(record(NOW.plus(Duration.ofMinutes(2))))
                .isEqualTo(RecordOutcome.RECORDED);
    }

    // Cancelling

    @Test
    void confirmedBookingIsCancelled() {
        clickMade(Duration.ofHours(2));
        when(conversionRepository.cancel(10L, BOOKING, NOW)).thenReturn(1);

        assertThat(conversionService.cancelConversion(TOKEN, BOOKING))
                .isEqualTo(CancelOutcome.CANCELLED);
    }

    @Test
    void cancellingTwiceChangesNothing() {
        clickMade(Duration.ofHours(2));
        when(conversionRepository.cancel(10L, BOOKING, NOW)).thenReturn(0);
        when(conversionRepository.findByCampaignIdAndBookingId(10L, BOOKING))
                .thenReturn(Optional.of(mock(AdConversion.class)));

        assertThat(conversionService.cancelConversion(TOKEN, BOOKING))
                .isEqualTo(CancelOutcome.ALREADY_CANCELLED);
    }

    @Test
    void cancellingAnUnknownBookingIsNotFound() {
        clickMade(Duration.ofHours(2));
        when(conversionRepository.cancel(10L, BOOKING, NOW)).thenReturn(0);
        when(conversionRepository.findByCampaignIdAndBookingId(10L, BOOKING))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversionService.cancelConversion(TOKEN, BOOKING))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Unknown conversion");
    }

    @Test
    void cancellingForAnUnknownClickIsNotFound() {
        when(clickRepository.findByClickToken("made-up")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversionService.cancelConversion("made-up", BOOKING))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Unknown click");

        verify(conversionRepository, never()).cancel(anyLong(), anyString(), any());
    }
}