package com.example.graphql.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.instrumentation.MicrometerGraphQlInstrumentation;

@Configuration
public class GraphQLMetricsConfig {
    @Bean
    public MicrometerGraphQlInstrumentation graphQlInstrumentation(MeterRegistry meterRegistry) {
        return new MicrometerGraphQlInstrumentation(meterRegistry);
    }
}
