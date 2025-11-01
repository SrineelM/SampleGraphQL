package com.example.graphql.config;

/*
 * NOTE: This configuration is disabled pending compatibility with Spring GraphQL 1.2.5.
 * The MicrometerGraphQlInstrumentation API may have changed or been moved in newer versions.
 * TODO: Update based on actual Spring GraphQL 1.2.5 metrics API when ready to implement.
 *
 * To enable GraphQL metrics:
 * 1. Check Spring GraphQL 1.2.5 documentation for instrumentation API
 * 2. Update imports and implementation
 * 3. Register the instrumentation bean
 */

// import io.micrometer.core.instrument.MeterRegistry;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.graphql.execution.instrumentation.MicrometerGraphQlInstrumentation;

// @Configuration
// public class GraphQLMetricsConfig {
//    @Bean
//     public MicrometerGraphQlInstrumentation graphQlInstrumentation(MeterRegistry meterRegistry) {
//         return new MicrometerGraphQlInstrumentation(meterRegistry);
//    }
// }
