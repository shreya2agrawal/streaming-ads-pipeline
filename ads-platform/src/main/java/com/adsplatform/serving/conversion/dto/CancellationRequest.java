package com.adsplatform.serving.conversion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancellationRequest(

    @NotBlank(message = "Click token is required")
    @Size(max = 64, message = "Click token must be at most 64 characters")
    String clickToken,

    @NotBlank(message = "Booking id is required")
    @Size(max = 100, message = "Booking id must be at most 100 characters")
    String bookingId) {
    
}
