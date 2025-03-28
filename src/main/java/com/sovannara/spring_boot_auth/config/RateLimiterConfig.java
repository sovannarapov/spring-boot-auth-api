package com.sovannara.spring_boot_auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.common.util.concurrent.RateLimiter;

@Configuration
public class RateLimiterConfig {
    @Bean
    public RateLimiter authenticationRateLimiter() {
        return RateLimiter.create(10.0); // 10 requests per second
    }
}
