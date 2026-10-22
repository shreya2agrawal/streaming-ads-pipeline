package com.adsplatform.serving.conversion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdConversionRepository extends JpaRepository<AdConversion, Long> {

    /*
     * Records the booking as CONFIRMED. Returns 1 the first time a booking
     * is reported for a campaign, and 0 for any repeat: the unique
     * constraint on (campaign_id, booking_id) lets only one insert win.
     */
    @Modifying
    @Query(value = """
            INSERT INTO ad_conversions
            (click_token, campaign_id, booking_id, value, currency,
            status, converted_at, received_at)
            VALUES
            (:clickToken, :campaignId, :bookingId, :value, :currency,
            'CONFIRMED', :convertedAt, :receivedAt)
            ON CONFLICT (campaign_id, booking_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("clickToken") String clickToken,
            @Param("campaignId") Long campaignId,
            @Param("bookingId") String bookingId,
            @Param("value") BigDecimal value,
            @Param("currency") String currency,
            @Param("convertedAt") Instant convertedAt,
            @Param("receivedAt") Instant receivedAt
        );

    /*
     * Cancels a confirmed booking. Returns 1 if it was cancelled now,
     * and 0 if it was already cancelled: the WHERE clause makes a
     * repeated cancellation change nothing.
     */
    @Modifying 
    @Query(value = """
        UPDATE ad_conversions
        SET status = 'CANCELLED', cancelled_at = :cancelledAt
        WHERE campaign_id = :campaignId
        AND booking_id = :bookingId
        AND status = 'CONFIRMED'    
        """, nativeQuery = true)
    int cancel(
        @Param("campaignId") Long campaignId,
        @Param("bookingId") String bookingId,
        @Param("cancelledAt") Instant cancelledAt
    );

    Optional<AdConversion> findByCampaignIdAndBookingId(Long campaignId, String bookingId);

}
