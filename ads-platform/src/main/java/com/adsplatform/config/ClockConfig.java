package com.adsplatform.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * One injectable source of "now". Code that asks the Clock instead of
 * calling Instant.now() directly can be tested at an exact moment.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}