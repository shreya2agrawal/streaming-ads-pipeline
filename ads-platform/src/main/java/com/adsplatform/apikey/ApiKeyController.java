package com.adsplatform.apikey;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adsplatform.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/advertiser-api-keys")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiResponse<IssuedKey>> issue(
            @Valid @RequestBody IssueKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(apiKeyService.issue(request.advertiser())));
    }

    @GetMapping 
    public ResponseEntity<ApiResponse<List<ApiKeySummary>>> list(
            @RequestParam String advertiser) {
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.list(advertiser)));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<ApiKeySummary>> revoke(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.ok(apiKeyService.revoke(id)));
    }
}
