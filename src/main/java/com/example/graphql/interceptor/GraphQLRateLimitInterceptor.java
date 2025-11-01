package com.example.graphql.interceptor;

/*
 * NOTE: This interceptor is disabled pending compatibility with Spring GraphQL 1.2.5.
 * The GraphQlInterceptor and related classes may have changed or been moved in newer versions.
 *
 * Rate limiting for GraphQL can be achieved through:
 * 1. Spring Security rules on the /graphql endpoint
 * 2. Custom WebFilter implementation
 * 3. Resilience4j RateLimiter on service methods
 *
 * TODO: Update based on actual Spring GraphQL 1.2.5 interceptor API when ready to implement.
 */

// import io.github.resilience4j.ratelimiter.RateLimiter;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.graphql.execution.GraphQlInterceptor;
// import org.springframework.graphql.execution.GraphQlRequest;
// import org.springframework.graphql.execution.GraphQlResponse;
// import org.springframework.stereotype.Component;
// import reactor.core.publisher.Mono;

// @Component
// public class GraphQLRateLimitInterceptor implements GraphQlInterceptor {
//    private final RateLimiter rateLimiter;

//     @Autowired
//     public GraphQLRateLimitInterceptor(RateLimiter graphQLRateLimiter) {
//         this.rateLimiter = graphQLRateLimiter;
//    }

//    @Override
//     public Mono<GraphQlResponse> intercept(GraphQlRequest request, Chain chain) {
//         if (!rateLimiter.acquirePermission()) {
//             return Mono.error(new RuntimeException("Rate limit exceeded. Try again later."));
//         }
//        return chain.next(request);
//     }
// }
