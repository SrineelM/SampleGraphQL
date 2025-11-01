# 📊 Observability Guide

**Monitoring, Metrics, Logging, and Tracing for GraphQL API**

---

## Quick Reference

| Component | Tool | Port | Purpose |
|-----------|------|------|---------|
| Metrics | Micrometer + Prometheus | 8080/actuator/prometheus | Time-series metrics |
| Logging | Logback + ELK | - | Centralized logs |
| Tracing | Spring Cloud Sleuth + Zipkin | 9411 | Distributed tracing |
| Health | Spring Actuator | 8080/actuator/health | Health checks |
| Dashboards | Grafana | 3000 | Visualization |

---

## 1. Metrics with Micrometer & Prometheus

### Add Dependencies
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

### Configure Actuator
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

### Custom GraphQL Metrics
```java
@Component
public class GraphQLMetrics {
    
    private final MeterRegistry registry;
    
    public GraphQLMetrics(MeterRegistry registry) {
        this.registry = registry;
        initializeMetrics();
    }
    
    private void initializeMetrics() {
        // Query execution time
        registry.timer("graphql.query.execution.time",
            Tags.of("operation", "query"));
        
        // Mutation execution time
        registry.timer("graphql.mutation.execution.time",
            Tags.of("operation", "mutation"));
        
        // Error counter
        registry.counter("graphql.errors.total",
            Tags.of("type", "error"));
        
        // Active requests
        registry.gauge("graphql.requests.active", 
            new AtomicInteger(0));
    }
    
    public void recordQueryTime(String queryName, long durationMs) {
        registry.timer("graphql.query.execution.time",
            Tags.of("query", queryName))
            .record(Duration.ofMillis(durationMs));
    }
    
    public void recordError(String errorType) {
        registry.counter("graphql.errors.total",
            Tags.of("type", errorType))
            .increment();
    }
}

@Component
public class GraphQLMetricsInterceptor implements WebGraphQlInterceptor {
    
    private final GraphQLMetrics metrics;
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        activeRequests.incrementAndGet();
        long startTime = System.currentTimeMillis();
        
        return chain.next(request)
            .doOnSuccess(response -> {
                long duration = System.currentTimeMillis() - startTime;
                String operationName = extractOperationName(request);
                metrics.recordQueryTime(operationName, duration);
            })
            .doOnError(error -> {
                metrics.recordError(error.getClass().getSimpleName());
            })
            .doFinally(signal -> activeRequests.decrementAndGet());
    }
}
```

---

## 2. Structured Logging

### Logback Configuration (logback-spring.xml)
```xml
<configuration>
    <springProperty scope="context" name="appName" source="spring.application.name"/>
    <springProperty scope="context" name="env" source="spring.profiles.active"/>
    
    <!-- Console Appender with JSON format -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app":"${appName}","env":"${env}"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
    </appender>
    
    <!-- File Appender with rotation -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### Add Logging Context
```java
@Component
public class LoggingInterceptor implements WebGraphQlInterceptor {
    
    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName)
            .defaultIfEmpty("anonymous")
            .flatMap(username -> {
                MDC.put("userId", username);
                MDC.put("operation", extractOperationName(request));
                MDC.put("requestId", UUID.randomUUID().toString());
                
                return chain.next(request)
                    .doFinally(signal -> MDC.clear());
            });
    }
}
```

---

## 3. Distributed Tracing

### Add Dependencies
```gradle
implementation 'org.springframework.cloud:spring-cloud-starter-sleuth'
implementation 'org.springframework.cloud:spring-cloud-sleuth-zipkin'
```

### Configure Tracing
```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # Sample 100% in dev, 0.1 (10%) in prod
  zipkin:
    base-url: http://localhost:9411
    enabled: true
```

### Custom Spans
```java
@Service
public class UserService {
    
    @Autowired
    private Tracer tracer;
    
    public User findById(Long id) {
        Span span = tracer.nextSpan().name("UserService.findById");
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            span.tag("user.id", id.toString());
            User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
            span.tag("user.found", "true");
            return user;
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

---

## 4. Health Checks

### Custom Health Indicators
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("status", "Connected")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}

@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection().ping();
            return Health.up()
                .withDetail("redis", "Connected")
                .withDetail("ping", pong)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

## 5. Docker Compose for Observability Stack

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
  
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
    depends_on:
      - prometheus
  
  zipkin:
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"
  
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data
  
  kibana:
    image: docker.elastic.co/kibana/kibana:8.10.0
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch

volumes:
  prometheus-data:
  grafana-data:
  es-data:
```

### Prometheus Configuration (prometheus.yml)
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-graphql'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

---

## 6. Grafana Dashboard

### Import Dashboard JSON
```json
{
  "dashboard": {
    "title": "GraphQL API Metrics",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(graphql_query_execution_time_count[5m])"
          }
        ]
      },
      {
        "title": "Average Response Time",
        "targets": [
          {
            "expr": "rate(graphql_query_execution_time_sum[5m]) / rate(graphql_query_execution_time_count[5m])"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(graphql_errors_total[5m])"
          }
        ]
      }
    ]
  }
}
```

---

## 7. Alerting Rules

### Prometheus Alert Rules (alerts.yml)
```yaml
groups:
  - name: graphql_alerts
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: rate(graphql_errors_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High GraphQL error rate"
          description: "Error rate is {{ $value }} errors/sec"
      
      - alert: SlowQueries
        expr: graphql_query_execution_time_seconds > 2
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Slow GraphQL queries detected"
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: warning
```

---

## 8. Quick Start

```bash
# Start observability stack
docker-compose up -d

# Access dashboards
open http://localhost:9090  # Prometheus
open http://localhost:3000  # Grafana (admin/admin)
open http://localhost:9411  # Zipkin
open http://localhost:5601  # Kibana

# View application metrics
curl http://localhost:8080/actuator/prometheus

# Check health
curl http://localhost:8080/actuator/health
```

---

*Monitor everything, alert on what matters, visualize for insights.*
