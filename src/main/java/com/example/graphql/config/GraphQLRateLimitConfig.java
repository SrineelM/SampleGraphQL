package com.example.graphql.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphQLRateLimitConfig {
    @Bean
    public RateLimiter graphQLRateLimiter() {
        return RateLimiter.of(
                "graphql",
                RateLimiterConfig.custom()
                        .limitForPeriod(100) // max 100 requests
                        .limitRefreshPeriod(Duration.ofMinutes(1))
                        .timeoutDuration(Duration.ofMillis(0))
                        .build());
    }
}
