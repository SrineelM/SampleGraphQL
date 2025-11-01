# ⚙️ Configuration Guide - Environment Setup

**Project**: SampleGraphQL  
**Document Date**: November 1, 2025

---

## Table of Contents
1. [Environment Overview](#environment-overview)
2. [Local Development (8GB Laptop)](#local-development-8gb-laptop)
3. [Development Environment](#development-environment)
4. [QA Environment](#qa-environment)
5. [Production Environment](#production-environment)
6. [Database Configuration](#database-configuration)
7. [Redis Configuration](#redis-configuration)
8. [Kafka Configuration](#kafka-configuration)

---

## Environment Overview

### Environment Matrix

| Environment | Database | Redis | Message Queue | Resource Limits |
|------------|----------|-------|---------------|----------------|
| **Local** | H2 (in-memory) | Mock/Embedded | Mock | 2GB RAM, 2 CPU |
| **Dev** | PostgreSQL (shared) | Redis (shared) | Kafka (dev cluster) | 4GB RAM, 4 CPU |
| **QA** | PostgreSQL (dedicated) | Redis (cluster) | Kafka (qa cluster) | 8GB RAM, 8 CPU |
| **Prod** | PostgreSQL (HA) | Redis (cluster) | Kafka (prod cluster) | 16GB RAM, 16 CPU |

---

## Local Development (8GB Laptop)

### application-local.yml

```yaml
# =================================================================
# LOCAL DEVELOPMENT CONFIGURATION (Optimized for 8GB Laptop)
# =================================================================
# Purpose: Minimal resource usage while maintaining functionality
# Database: H2 in-memory (no setup required)
# Redis: Embedded or mock
# Services: All mocked to avoid external dependencies
# =================================================================

spring:
  config:
    activate:
      on-profile: local
  
  # H2 In-Memory Database - Zero setup required
  datasource:
    url: jdbc:h2:mem:localdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
    hikari:
      # Minimal connection pool for local development
      minimum-idle: 2                  # Min idle connections
      maximum-pool-size: 5             # Max connections (vs 20 in prod)
      connection-timeout: 30000        # 30s timeout
      idle-timeout: 300000             # 5min idle timeout
      max-lifetime: 600000             # 10min max lifetime
      pool-name: LocalHikariPool
      
  jpa:
    hibernate:
      ddl-auto: create-drop            # Auto-create schema on startup
    show-sql: true                     # Show SQL for debugging
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
        jdbc:
          batch_size: 10               # Smaller batch size
        order_inserts: true
        
  h2:
    console:
      enabled: true                    # Enable H2 console
      path: /h2-console
      settings:
        web-allow-others: false
        trace: false
  
  # Embedded Redis (no separate server needed)
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 1000
      # Use embedded Redis or skip if not available
      
  cache:
    type: simple                       # Use simple in-memory cache (not Redis)
    # Alternatively: type: caffeine for better performance
    
  # GraphQL Configuration
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
    websocket:
      path: /graphql-ws
    query-depth: 10                    # Lower limit for local
    query-complexity: 100
    
# JWT Configuration - Development only, not for production!
jwt:
  secret: local-dev-secret-key-12345678901234567890
  expiration: 86400000                 # 24 hours (longer for convenience)
  refresh-expiration: 604800000        # 7 days
  
# Server Configuration - Lightweight
server:
  port: 8080
  shutdown: graceful
  tomcat:
    threads:
      max: 50                          # Reduced from 200
      min-spare: 5
    max-connections: 1000              # Reduced from 10000
    accept-count: 50
  compression:
    enabled: true                      # Enable compression
    min-response-size: 1024
    
# Logging - Verbose for development
logging:
  level:
    root: INFO
    com.example.graphql: DEBUG
    org.springframework: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type: TRACE
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    
# Resilience4j - Relaxed for local development
resilience4j:
  circuitbreaker:
    instances:
      default:
        sliding-window-size: 5
        failure-rate-threshold: 75     # Higher threshold
        wait-duration-in-open-state: 10s
  retry:
    instances:
      default:
        max-attempts: 2                # Fewer retries
        wait-duration: 500ms
  ratelimiter:
    instances:
      default:
        limit-for-period: 100
        limit-refresh-period: 1s
        
# Actuator - All endpoints enabled
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: false                 # Disable to save resources
        
# Mock External Services
external:
  services:
    mock-mode: true                    # Use mock implementations
    timeout: 5000
```

### Docker Compose for Local (Optional)

If you want to test with real PostgreSQL and Redis locally:

```yaml
# docker-compose.local.yml
version: '3.8'

services:
  postgres-local:
    image: postgres:15-alpine
    container_name: graphql-postgres-local
    environment:
      POSTGRES_DB: localdb
      POSTGRES_USER: localuser
      POSTGRES_PASSWORD: localpass
    ports:
      - "5432:5432"
    volumes:
      - postgres-local-data:/var/lib/postgresql/data
    mem_limit: 512m                    # Limit memory
    cpus: 1                            # Limit CPU
    
  redis-local:
    image: redis:7-alpine
    container_name: graphql-redis-local
    ports:
      - "6379:6379"
    mem_limit: 256m
    cpus: 0.5
    
volumes:
  postgres-local-data:
```

**Start local services:**
```bash
docker-compose -f docker-compose.local.yml up -d
```

**Stop local services:**
```bash
docker-compose -f docker-compose.local.yml down
```

---

## Development Environment

### application-dev.yml (Enhanced)

```yaml
# =================================================================
# DEVELOPMENT ENVIRONMENT CONFIGURATION
# =================================================================
# Purpose: Team development environment with shared resources
# Database: PostgreSQL (shared dev instance)
# Redis: Redis (shared dev instance)
# Services: Real external services in dev mode
# =================================================================

spring:
  config:
    activate:
      on-profile: dev
  
  # PostgreSQL Database
  datasource:
    url: jdbc:postgresql://${DB_HOST:dev-postgres.company.com}:5432/${DB_NAME:graphql_dev}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME:devuser}
    password: ${DB_PASSWORD}           # From environment variable
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: DevHikariPool
      leak-detection-threshold: 60000  # Detect connection leaks
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        
  jpa:
    hibernate:
      ddl-auto: validate               # Validate schema (use Flyway for migrations)
    show-sql: false                    # Disable SQL logging in dev
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 25
        order_inserts: true
        order_updates: true
        # Enable second-level cache
        cache:
          use_second_level_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
            
  # Flyway Database Migration
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    
  # Redis Configuration
  data:
    redis:
      host: ${REDIS_HOST:dev-redis.company.com}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      database: 0
      timeout: 2000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
          
  cache:
    type: redis
    redis:
      time-to-live: 600000             # 10 minutes
      cache-null-values: false
      
# JWT Configuration
jwt:
  secret: ${JWT_SECRET}                # From environment variable
  expiration: 3600000                  # 1 hour
  refresh-expiration: 86400000         # 24 hours
  
# Server Configuration
server:
  port: 8080
  tomcat:
    threads:
      max: 100
      min-spare: 10
    max-connections: 5000
    
# Logging - Structured JSON format
logging:
  level:
    root: INFO
    com.example.graphql: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    
# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
      
# External Services (Real endpoints in dev)
external:
  services:
    mock-mode: false
    api-base-url: https://dev-api.company.com
    timeout: 10000
```

---

## QA Environment

### application-qa.yml

```yaml
# =================================================================
# QA ENVIRONMENT CONFIGURATION
# =================================================================
# Purpose: Pre-production testing with production-like setup
# Database: PostgreSQL (dedicated QA instance)
# Redis: Redis Cluster
# Services: QA versions of external services
# =================================================================

spring:
  config:
    activate:
      on-profile: qa
  
  # PostgreSQL Database - Dedicated instance
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      minimum-idle: 10
      maximum-pool-size: 30
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1800000
      pool-name: QAHikariPool
      leak-detection-threshold: 30000
      
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 25
          
  # Flyway
  flyway:
    enabled: true
    locations: classpath:db/migration
    
  # Redis Cluster Configuration
  data:
    redis:
      cluster:
        nodes:
          - ${REDIS_NODE1}:6379
          - ${REDIS_NODE2}:6379
          - ${REDIS_NODE3}:6379
        max-redirects: 3
      password: ${REDIS_PASSWORD}
      timeout: 2000
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
          
  cache:
    type: redis
    redis:
      time-to-live: 300000             # 5 minutes
      
# JWT Configuration
jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000                  # 1 hour
  refresh-expiration: 86400000
  
# Server Configuration
server:
  port: 8080
  tomcat:
    threads:
      max: 150
      min-spare: 20
    max-connections: 8000
    
# Kafka Configuration (for QA testing)
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    consumer:
      group-id: graphql-qa
      auto-offset-reset: earliest
    producer:
      acks: all
      retries: 3
      
# Logging - JSON format for log aggregation
logging:
  level:
    root: INFO
    com.example.graphql: INFO
  pattern:
    console: '{"timestamp":"%d{ISO8601}","level":"%p","logger":"%c","message":"%m"}%n'
    
# Metrics & Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      environment: qa
      application: graphql-api
```

---

## Production Environment

### application-prod.yml

```yaml
# =================================================================
# PRODUCTION ENVIRONMENT CONFIGURATION
# =================================================================
# Purpose: Production deployment with HA and security
# Database: PostgreSQL (High Availability setup)
# Redis: Redis Cluster (HA)
# Services: Production external services
# Security: All secrets from external vault
# =================================================================

spring:
  config:
    activate:
      on-profile: prod
  
  # PostgreSQL HA Configuration
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}?ssl=true&sslmode=require
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}           # From AWS Secrets Manager or Vault
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50            # Higher for production load
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1800000
      pool-name: ProdHikariPool
      leak-detection-threshold: 15000  # Faster leak detection
      connection-init-sql: "SELECT 1"  # Validate connections
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 500
        prepStmtCacheSqlLimit: 4096
        useServerPrepStmts: true
        reWriteBatchedInserts: true    # PostgreSQL optimization
        
  jpa:
    hibernate:
      ddl-auto: validate               # Never auto-modify prod schema
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 50               # Larger batches
          fetch_size: 50
        order_inserts: true
        order_updates: true
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
            
  # Flyway
  flyway:
    enabled: true
    locations: classpath:db/migration
    validate-on-migrate: true
    baseline-on-migrate: false
    
  # Redis Cluster (HA)
  data:
    redis:
      cluster:
        nodes:
          - ${REDIS_NODE1}:6379
          - ${REDIS_NODE2}:6379
          - ${REDIS_NODE3}:6379
          - ${REDIS_NODE4}:6379
          - ${REDIS_NODE5}:6379
          - ${REDIS_NODE6}:6379
        max-redirects: 5
      password: ${REDIS_PASSWORD}
      ssl: true
      timeout: 2000
      lettuce:
        pool:
          max-active: 32
          max-idle: 16
          min-idle: 8
          max-wait: 1000ms
        cluster:
          refresh:
            adaptive: true
            period: 30s
            
  cache:
    type: redis
    redis:
      time-to-live: 300000
      cache-null-values: false
      key-prefix: "graphql:"
      use-key-prefix: true
      
# JWT Configuration (Production)
jwt:
  secret: ${JWT_SECRET}                # From AWS Secrets Manager
  expiration: 900000                   # 15 minutes (short-lived)
  refresh-expiration: 604800000        # 7 days
  
# Server Configuration (Production)
server:
  port: 8080
  shutdown: graceful
  tomcat:
    threads:
      max: 200
      min-spare: 25
    max-connections: 10000
    connection-timeout: 20000
    accept-count: 100
  compression:
    enabled: true
    min-response-size: 1024
    mime-types: application/json,application/graphql
    
# Kafka Configuration (Production)
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
    consumer:
      group-id: graphql-prod
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 500
    producer:
      acks: all
      retries: 3
      compression-type: snappy
      batch-size: 16384
      linger-ms: 10
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: SCRAM-SHA-512
      sasl.jaas.config: ${KAFKA_JAAS_CONFIG}
      
# Security
security:
  require-ssl: true
  cors:
    allowed-origins: ${CORS_ORIGINS}
    allowed-methods: POST,GET,OPTIONS
    allowed-headers: Authorization,Content-Type
    allow-credentials: true
    max-age: 3600
    
# Logging (Production)
logging:
  level:
    root: WARN
    com.example.graphql: INFO
    org.springframework.security: WARN
  pattern:
    console: '{"timestamp":"%d{ISO8601}","level":"%p","logger":"%c","message":"%replace(%m){'[\r\n]',''}","trace_id":"%X{traceId}","span_id":"%X{spanId}"}%n'
    
# Metrics & Observability
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: never              # Hide details in production
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      environment: production
      application: graphql-api
      region: ${AWS_REGION}
  tracing:
    sampling:
      probability: 0.1                 # 10% sampling
      
# Resilience4j (Production)
resilience4j:
  circuitbreaker:
    instances:
      default:
        sliding-window-size: 100
        minimum-number-of-calls: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 10
  retry:
    instances:
      default:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
  timelimiter:
    instances:
      default:
        timeout-duration: 5s
  bulkhead:
    instances:
      default:
        max-concurrent-calls: 25
        max-wait-duration: 500ms
```

---

## Database Configuration

### PostgreSQL Setup

**Development:**
```bash
# Create database
CREATE DATABASE graphql_dev;

# Create user
CREATE USER devuser WITH ENCRYPTED PASSWORD 'devpassword';

# Grant permissions
GRANT ALL PRIVILEGES ON DATABASE graphql_dev TO devuser;
```

**Production:**
```bash
# Use managed service (AWS RDS, Azure Database for PostgreSQL, Google Cloud SQL)
# Enable:
# - Multi-AZ deployment
# - Automated backups
# - Read replicas for scaling
# - SSL connections
# - Connection pooling (PgBouncer)
```

---

## Redis Configuration

### Redis Cluster Setup

**Production:**
```bash
# Use managed Redis (AWS ElastiCache, Azure Cache for Redis)
# Configuration:
# - Cluster mode enabled
# - 6 nodes (3 primary, 3 replica)
# - Automatic failover
# - Encryption in transit
# - Encryption at rest
```

---

## Kafka Configuration

### Topics Setup

```bash
# Create topics for production
kafka-topics --create \
  --bootstrap-server ${KAFKA_BROKERS} \
  --topic graphql.events \
  --partitions 6 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config retention.ms=259200000

kafka-topics --create \
  --bootstrap-server ${KAFKA_BROKERS} \
  --topic graphql.audit \
  --partitions 3 \
  --replication-factor 3
```

---

## Environment Variables

### Required Environment Variables

**Development:**
```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=localhost
export DB_NAME=graphql_dev
export DB_USERNAME=devuser
export DB_PASSWORD=devpassword
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=your-dev-secret-key-min-32-chars
```

**Production:**
```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=prod-postgres.region.rds.amazonaws.com
export DB_NAME=graphql_prod
export DB_USERNAME=produser
export DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id db-password --query SecretString --output text)
export REDIS_NODE1=redis-001.cache.amazonaws.com
export REDIS_NODE2=redis-002.cache.amazonaws.com
export REDIS_PASSWORD=$(aws secretsmanager get-secret-value --secret-id redis-password --query SecretString --output text)
export JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id jwt-secret --query SecretString --output text)
export KAFKA_BROKERS=broker1:9092,broker2:9092,broker3:9092
```

---

## Quick Start Guide

### Local (8GB Laptop)
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Development
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Production
```bash
java -jar -Xmx4g -Xms4g \
  -Dspring.profiles.active=prod \
  -Dserver.port=8080 \
  build/libs/graphql-poc-1.0.0.jar
```

---

*Configuration is key to deployment success!*
