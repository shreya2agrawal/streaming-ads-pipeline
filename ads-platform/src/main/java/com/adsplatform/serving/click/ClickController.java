package com.adsplatform.serving.click;

import java.net.URI;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/*
 * Every ad click passes through here before reaching the advertiser,
 * so it is counted and charged.
 *
 * The redirect is never cached: a cached redirect would send later
 * clicks straight to the advertiser, uncounted.
 */
@RestController
@RequestMapping("/api/v1/clicks")
@RequiredArgsConstructor
public class ClickController {

    private final ClickService clickService;

    @GetMapping("/{clickToken}")
    public ResponseEntity<Void> click(@PathVariable String clickToken) {
        ClickResult result = clickService.recordClick(clickToken);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(result.destinationUrl()))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}