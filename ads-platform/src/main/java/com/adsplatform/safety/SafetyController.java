package com.adsplatform.safety;

import com.adsplatform.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;
    private final VectorSafetyService vectorSafetyService;

    @GetMapping("/flags")
    public ResponseEntity<ApiResponse<List<SafetyFlag>>> getPending(){
        return ResponseEntity.ok(ApiResponse.ok(safetyService.getPendingFlags()));
    }

    @PostMapping("/flags")
    public ResponseEntity<ApiResponse<SafetyFlag>> createFlag(@RequestBody SafetyFlag flag){
        return ResponseEntity.ok(ApiResponse.ok(safetyService.createFlag(flag)));
    }

    @PatchMapping("/flags/{id}/review")
    public ResponseEntity<ApiResponse<SafetyFlag>> review(
            @PathVariable Long id,
            @RequestParam String reviewer,
            @RequestParam boolean approved
    ){
        return ResponseEntity.ok(ApiResponse.ok(safetyService.reviewFlag(id, reviewer, approved)));
    }

    @GetMapping("/flags/pending-count")
    public ResponseEntity<ApiResponse<Long>> pendingCount(){
        return ResponseEntity.ok(ApiResponse.ok(safetyService.getPendingCount()));
    }

    @PostMapping("/score")
    public ResponseEntity<ApiResponse<VectorSafetyResult>> scoreCreative(
            @RequestParam String creativeText,
            @RequestParam String advertiser,
            @RequestParam(required = false) String targetShowId
    ){
        VectorSafetyResult result = vectorSafetyService.scoreAdCreative(
                creativeText, advertiser, targetShowId
        );
        if(result.getDecision() != VectorSafetyResult.SafetyDecision.SAFE){
            SafetyFlag flag = SafetyFlag.builder()
                    .creativeFile(creativeText.substring(0, Math.min(60, creativeText.length())))
                    .advertiser(advertiser)
                    .reason(result.getReason())
                    .severity(result.getDecision() == VectorSafetyResult.SafetyDecision.BLOCK
                    ? SafetyFlag.Severity.POLICY_VIOLATION
                            : SafetyFlag.Severity.ADVISORY)
                    .build();
            safetyService.createFlag(flag);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
