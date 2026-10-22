package com.adsplatform.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IssueKeyRequest(

    @NotBlank(message = "Advertiser is required")
    @Size(max = 255, message = "Advertiser must be at most 255 characters")
    String advertiser) {
}
