package com.adsplatform.creative.dto;

import com.adsplatform.creative.CreativeType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.URL;

public record UpdateCreativeRequest(

    @NotBlank(message = "Creative name is required")
    String name,

    @NotNull(message = "Creative type is required")
    CreativeType creativeType,

    @NotBlank(message = "Media URL is required")
    @URL(message = "Media URL must be a valid URL")
    String mediaUrl,

    @NotBlank(message = "Destination URL is required")
    @URL(message = "Destination URL must be a valid URL")
    String destinationUrl,

    @NotBlank(message = "Creative content is required")
    String contentText,

    @Positive(message = "Duration must be greater than zero")
    Integer durationSeconds

) {
}