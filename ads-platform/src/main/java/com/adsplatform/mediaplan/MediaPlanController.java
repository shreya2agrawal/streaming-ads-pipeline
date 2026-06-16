package com.adsplatform.mediaplan;

import com.adsplatform.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media-plans")
@CrossOrigin(origins = "http://localhost:3000")
public class MediaPlanController {

    private final MediaPlanService mediaPlanService;

//    CRUD ------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaPlan>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.getAllPlans()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaPlan>> getPlanById(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.getPlanById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MediaPlan>> create(@RequestBody MediaPlan plan){
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.createPlan(plan)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaPlan>> update(
            @PathVariable Long id,
            @RequestBody MediaPlan plan){
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.updatePlan(id, plan)));
    }

//     Workflow endpoint -----------------------------------------------------------------

    @PostMapping("/{id}/send-proposal")
    public ResponseEntity<ApiResponse<MediaPlan>> sendProposal(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.sendProposal(id)));
    }

    @PostMapping("/{id}/negotiate")
    public ResponseEntity<ApiResponse<MediaPlan>> negotiate(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.startNegotiation(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<MediaPlan>> approve(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.approvePlan(id)));
    }

    @PostMapping("/{id}/allocate-inventory")
    public ResponseEntity<ApiResponse<MediaPlan>> allocateInventory(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                mediaPlanService.allocateInventory(id)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<MediaPlan>> activate(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.activatePlan(id)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<MediaPlan>> complete(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.completePlan(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<MediaPlan>> cancel(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.cancelPlan(id)));
    }

//    Analytics ---------------------------------------------------------------------------

    @GetMapping("/pipeline")
    public ResponseEntity<ApiResponse<List<MediaPlan>>> pipeline() {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.getPipeline()));
    }

    @GetMapping("/under-delivering")
    public ResponseEntity<ApiResponse<List<MediaPlan>>> underDelivering(
            @RequestParam(defaultValue = "80.0") double threshold) {
        return ResponseEntity.ok(ApiResponse.ok(
                mediaPlanService.getUnderDeliveringPlans(threshold)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MediaPlanService.MediaPlanStats>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(mediaPlanService.getStats()));
    }

}
