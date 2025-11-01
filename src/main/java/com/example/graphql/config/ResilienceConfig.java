package com.example.graphql.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience Configuration for the application.
 *
 * <p>Configures circuit breakers and rate limiters to handle transient failures and prevent
 * cascading failures across the system.
 *
 * <p>Key patterns implemented:
 * - Circuit Breaker: Stops requests when downstream service is failing
 * - Rate Limiter: Prevents overwhelming downstream services
 *
 * @see io.github.resilience4j.circuitbreaker.CircuitBreaker
 * @see io.github.resilience4j.ratelimiter.RateLimiter
 */
@Configuration
public class ResilienceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);

    /**
     * Configures the CircuitBreakerRegistry with custom settings.
     *
     * <p>A circuit breaker prevents cascading failures by stopping requests when a threshold of
     * failures is reached. It transitions through states:
     * - CLOSED: Normal operation, requests pass through
     * - OPEN: Failure threshold reached, requests are rejected immediately
     * - HALF_OPEN: Periodic test requests to check if service recovered
     *
     * @return CircuitBreakerRegistry configured with sensible defaults
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Default configuration for all circuit breakers
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f) // Open after 50% failures
                .slowCallRateThreshold(50.0f) // Slow call threshold
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // Requests taking >2s are "slow"
                .waitDurationInOpenState(Duration.ofSeconds(10)) // Wait 10s before trying again
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open
                .minimumNumberOfCalls(5) // Need at least 5 calls to evaluate
                .automaticTransitionFromOpenToHalfOpenEnabled(true) // Auto-transition
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Add event logging for debugging
        registry.getEventPublisher()
                .onEntryAdded(event -> logger.info("CircuitBreaker {} registered", event.getAddedEntry().getName()))
                .onEntryRemoved(event -> logger.info("CircuitBreaker {} removed", event.getRemovedEntry().getName()));

        return registry;
    }

    /**
     * Configures the RateLimiterRegistry with custom settings.
     *
     * <p>Rate limiting prevents overwhelming downstream services by limiting the number of
     * requests per time window.
     *
     * @return RateLimiterRegistry configured with sensible defaults
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Default configuration: 100 requests per second
        RateLimiterConfig defaultConfig =
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1)) // Reset every 1 second
                        .limitForPeriod(100) // Allow 100 requests per period
                        .timeoutDuration(Duration.ofMillis(100)) // Wait max 100ms for a permit
                        .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);

        // Add event logging for debugging
        registry.getEventPublisher()
                .onEntryAdded(event -> logger.info("RateLimiter {} registered", event.getAddedEntry().getName()))
                .onEntryRemoved(
                        event -> logger.info("RateLimiter {} removed", event.getRemovedEntry().getName()));

        return registry;
    }
}
