package com.adsplatform.serving.conversion.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConversionRequest(
    @NotBlank(message = "Click token is required")
    @Size(max = 64, message = "Click token must be at most 64 characters")
    String clickToken,

    @NotBlank(message = "Booking id is required")
    @Size(max = 100, message = "Booking id must be at most 100 characters")
    String bookingId,

    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.00", message = "Value must not be negative")
    @Digits(integer = 10, fraction = 2, message = "Value must have at most 2 decimal places")
    BigDecimal value,

    @NotNull(message = "Currency is required")
    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter code like USD")
    String currency,

    @NotNull(message = "Conversion time is required")
    Instant convertedAt
) {
    
}
