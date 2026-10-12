package com.adsplatform.creative;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adsplatform.common.ApiResponse;
import com.adsplatform.creative.dto.CreateCreativeRequest;
import com.adsplatform.creative.dto.CreativeResponse;
import com.adsplatform.creative.dto.UpdateCreativeRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/creatives")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class CreativeController {

    private final CreativeService creativeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CreativeResponse>>> getAllCreatives() {
        return ResponseEntity.ok(
                ApiResponse.ok(creativeService.getAllCreatives()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreativeResponse>> getCreative(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.ok(creativeService.getCreative(id)));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<ApiResponse<List<CreativeResponse>>> getCreativesByCampaign(
            @PathVariable Long campaignId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        creativeService.getCreativesByCampaign(campaignId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreativeResponse>> createCreative(
            @Valid @RequestBody CreateCreativeRequest request) {

        CreativeResponse createdCreative = creativeService.createCreative(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(createdCreative));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CreativeResponse>> updateCreative(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCreativeRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        creativeService.updateCreative(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CreativeResponse>> updateCreativeStatus(
            @PathVariable Long id,
            @RequestParam CreativeStatus status) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        creativeService.updateStatus(id, status)));
    }
}