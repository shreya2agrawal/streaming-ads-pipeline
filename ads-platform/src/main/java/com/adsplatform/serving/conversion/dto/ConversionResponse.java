package com.adsplatform.serving.conversion.dto;

/*
 * What happened to the report: RECORDED, DUPLICATE, OUTSIDE_WINDOW,
 * CANCELLED or ALREADY_CANCELLED. Lets the advertiser's server see
 * that a retry was harmless.
 */
public record ConversionResponse(String status) {
    
}
