package com.adsplatform.serving.click;

import java.math.BigDecimal;

/*
 * What happened to one click. The viewer is redirected in every case;
 * only CHARGED changes the campaign's spend.
 */
public record ClickResult(
        String destinationUrl,
        Outcome outcome,
        BigDecimal chargedAmount) {

    public enum Outcome {
        CHARGED,    // first click; amount may be 0 if the budget is spent
        DUPLICATE,  // same token clicked before: not charged again
        EXPIRED     // token older than its lifetime: not charged
    }

    static ClickResult charged(String destinationUrl, BigDecimal amount) {
        return new ClickResult(destinationUrl, Outcome.CHARGED, amount);
    }

    static ClickResult duplicate(String destinationUrl) {
        return new ClickResult(destinationUrl, Outcome.DUPLICATE, BigDecimal.ZERO);
    }

    static ClickResult expired(String destinationUrl) {
        return new ClickResult(destinationUrl, Outcome.EXPIRED, BigDecimal.ZERO);
    }
}