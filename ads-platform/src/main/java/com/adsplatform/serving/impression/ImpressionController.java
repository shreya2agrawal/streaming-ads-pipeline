package com.adsplatform.serving.impression;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/impressions")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor 
public class ImpressionController {

    private final ImpressionService impressionService;

    @PostMapping("/{slotToken}")
    public ResponseEntity<Void> recordImpression(@PathVariable String slotToken){
        impressionService.recordImpression(slotToken);
        return ResponseEntity.noContent().build();
    }
    
}
