package com.example.graphql.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.GraphQlInterceptor;
import org.springframework.graphql.execution.GraphQlRequest;
import org.springframework.graphql.execution.GraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GraphQLRateLimitInterceptor implements GraphQlInterceptor {
    private final RateLimiter rateLimiter;

    @Autowired
    public GraphQLRateLimitInterceptor(RateLimiter graphQLRateLimiter) {
        this.rateLimiter = graphQLRateLimiter;
    }

    @Override
    public Mono<GraphQlResponse> intercept(GraphQlRequest request, Chain chain) {
        if (!rateLimiter.acquirePermission()) {
            return Mono.error(new RuntimeException("Rate limit exceeded. Try again later."));
        }
        return chain.next(request);
    }
}
