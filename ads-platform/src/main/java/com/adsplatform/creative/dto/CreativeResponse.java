package com.adsplatform.creative.dto;

import java.time.LocalDateTime;

import com.adsplatform.creative.CreativeSafetyStatus;
import com.adsplatform.creative.CreativeStatus;
import com.adsplatform.creative.CreativeType;

public record CreativeResponse(

    Long id,
    Long campaignId,
    String name,
    CreativeType creativeType,
    String mediaUrl,
    String destinationUrl,
    String contentText,
    Integer durationSeconds,
    CreativeStatus status,
    CreativeSafetyStatus safetyStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt

) {
}