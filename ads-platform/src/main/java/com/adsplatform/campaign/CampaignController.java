package com.adsplatform.campaign;

import com.adsplatform.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "http://localhost:3000/")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Campaign>>> getAll(){
        return ResponseEntity.ok(ApiResponse.ok(campaignService.getAllCampaigns()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Campaign>> getById(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.ok(campaignService.getCampaignById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Campaign>> create(@RequestBody Campaign campaign){
        return ResponseEntity.ok(ApiResponse.ok(campaignService.createCampaign(campaign)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Campaign>> update(
            @PathVariable Long id,
            @RequestBody Campaign campaign
    ){
        return ResponseEntity.ok(ApiResponse.ok(campaignService.updateCampaign(id, campaign)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id){
        campaignService.deleteCampaign(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/under-pacing")
    public ResponseEntity<ApiResponse<List<Campaign>>> underPacing(
            @RequestParam(defaultValue = "50.0") double threshold){
        return ResponseEntity.ok(ApiResponse.ok(campaignService.getUnderPacingCampaigns(threshold)));
    }

    @GetMapping("/stats/active-count")
    public ResponseEntity<ApiResponse<Long>> activeCount(){
        return ResponseEntity.ok(ApiResponse.ok(campaignService.countActive()));
    }
}
