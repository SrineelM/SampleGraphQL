-- ===========================================================================================
-- COMPREHENSIVE TEST DATA FOR GRAPHQL POC
-- ===========================================================================================
-- Purpose: Provides realistic test data for all API endpoints
-- Passwords: All users have password "Password123!" (BCrypt hashed)
-- Usage: Automatically loaded by Spring Boot on application startup
-- ===========================================================================================

-- ===========================
-- USERS (10 users with different roles)
-- ===========================
-- Note: Password for all users is "Password123!"
-- BCrypt hash: $2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ

INSERT INTO users (id, username, email, password, role, created_at, updated_at) VALUES
-- Admin user for testing admin operations
(1, 'admin', 'admin@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_ADMIN', DATEADD('DAY', -90, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Moderators for testing moderation features
(2, 'moderator_sarah', 'sarah.mod@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_MODERATOR', DATEADD('DAY', -75, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(3, 'moderator_mike', 'mike.mod@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_MODERATOR', DATEADD('DAY', -60, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Regular users with various activity levels
(4, 'john_doe', 'john.doe@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_USER', DATEADD('DAY', -50, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(5, 'jane_smith', 'jane.smith@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_USER', DATEADD('DAY', -45, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(6, 'alice_wonder', 'alice.wonder@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_USER', DATEADD('DAY', -30, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(7, 'bob_builder', 'bob.builder@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_USER', DATEADD('DAY', -25, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(8, 'charlie_brown', 'charlie.brown@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_USER', DATEADD('DAY', -15, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- New users (recently joined)
(9, 'david_new', 'david.new@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_USER', DATEADD('DAY', -5, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(10, 'emma_fresh', 'emma.fresh@example.com', '$2a$12$LQbK9wLgN5vXEP7rX5rJVeF0gP9nQ7Z5gP5nQ7Z5gP5nQ7Z5gP5nQ', 'ROLE_USER', DATEADD('DAY', -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP);

-- ===========================
-- POSTS (30 posts with varied content and timestamps)
-- ===========================

-- Admin's posts (technical guides)
INSERT INTO posts (id, title, content, user_id, created_at, updated_at) VALUES
(1, 'Getting Started with GraphQL', 
    'GraphQL is a query language for APIs and a runtime for executing those queries. This comprehensive guide will walk you through the basics of GraphQL, including queries, mutations, subscriptions, and best practices for schema design.', 
    1, DATEADD('DAY', -80, CURRENT_TIMESTAMP), DATEADD('DAY', -75, CURRENT_TIMESTAMP)),

(2, 'Spring Boot 3 Migration Guide', 
    'Migrating to Spring Boot 3 requires understanding the breaking changes and new features. This guide covers Jakarta EE migration, GraalVM native images, and observability improvements.', 
    1, DATEADD('DAY', -70, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(3, 'Microservices Architecture Patterns', 
    'Explore key microservices patterns including API Gateway, Service Discovery, Circuit Breaker, Event Sourcing, and CQRS with practical Spring Boot examples.', 
    1, DATEADD('DAY', -60, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Moderator Sarah's posts (security focused)
(4, 'JWT Authentication Best Practices', 
    'Learn how to implement secure JWT authentication in Spring Boot applications. Covers token generation, validation, refresh tokens, and common security pitfalls.', 
    2, DATEADD('DAY', -55, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(5, 'Preventing GraphQL Injection Attacks', 
    'Security guide for GraphQL APIs covering query complexity limits, depth restrictions, input validation, and rate limiting strategies.', 
    2, DATEADD('DAY', -50, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(6, 'OAuth 2.0 and OpenID Connect', 
    'Complete guide to implementing OAuth 2.0 and OpenID Connect in Spring Security 6 with practical examples.', 
    2, DATEADD('DAY', -45, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Moderator Mike's posts (performance focused)
(7, 'Redis Caching Strategies', 
    'Optimize your Spring Boot application with Redis caching. Covers cache-aside, write-through, write-behind patterns and TTL configuration.', 
    3, DATEADD('DAY', -48, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(8, 'Solving the N+1 Query Problem', 
    'Deep dive into the N+1 query problem in GraphQL and how to solve it using DataLoader pattern with Spring Boot.', 
    3, DATEADD('DAY', -42, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(9, 'Database Connection Pooling with HikariCP', 
    'Optimize database performance with HikariCP connection pooling. Learn about pool sizing, connection timeout, and monitoring.', 
    3, DATEADD('DAY', -38, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- John Doe's posts (backend development)
(10, 'Building REST APIs with Spring Boot', 
    'Step-by-step guide to building RESTful APIs with Spring Boot including CRUD operations, exception handling, and API versioning.', 
    4, DATEADD('DAY', -40, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(11, 'Understanding Spring Data JPA', 
    'Master Spring Data JPA with examples covering repositories, query methods, specifications, and custom implementations.', 
    4, DATEADD('DAY', -35, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(12, 'Event-Driven Architecture with Spring', 
    'Implement event-driven systems using Spring Events, Spring Cloud Stream, and Apache Kafka.', 
    4, DATEADD('DAY', -28, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Jane Smith's posts (testing and quality)
(13, 'Unit Testing with JUnit 5 and Mockito', 
    'Comprehensive testing guide covering unit tests, integration tests, test doubles, and achieving high code coverage.', 
    5, DATEADD('DAY', -37, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(14, 'GraphQL Integration Testing', 
    'Learn how to test GraphQL APIs using GraphQlTester, Testcontainers, and Spring Boot test slices.', 
    5, DATEADD('DAY', -32, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(15, 'Performance Testing with Gatling', 
    'Load testing your APIs with Gatling. Includes scenario creation, metrics analysis, and performance optimization.', 
    5, DATEADD('DAY', -25, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Alice Wonder's posts (DevOps and deployment)
(16, 'Docker Best Practices for Java', 
    'Optimize Docker images for Java applications with multi-stage builds, layer caching, and security scanning.', 
    6, DATEADD('DAY', -30, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(17, 'Kubernetes Deployment Strategies', 
    'Deploy Spring Boot applications to Kubernetes with health checks, resource limits, and horizontal pod autoscaling.', 
    6, DATEADD('DAY', -24, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(18, 'CI/CD with GitHub Actions', 
    'Automate your deployment pipeline with GitHub Actions including testing, building, and deploying to cloud platforms.', 
    6, DATEADD('DAY', -18, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Bob Builder's posts (frontend integration)
(19, 'Integrating React with GraphQL', 
    'Build modern React applications with Apollo Client for GraphQL queries, mutations, and real-time subscriptions.', 
    7, DATEADD('DAY', -22, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(20, 'TypeScript for Java Developers', 
    'Transition from Java to TypeScript with this comprehensive guide covering types, interfaces, and async patterns.', 
    7, DATEADD('DAY', -16, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(21, 'Building Progressive Web Apps', 
    'Create offline-capable PWAs with service workers, caching strategies, and push notifications.', 
    7, DATEADD('DAY', -12, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Charlie Brown's posts (monitoring and observability)
(22, 'Monitoring with Prometheus and Grafana', 
    'Set up comprehensive monitoring for Spring Boot applications using Micrometer, Prometheus, and Grafana dashboards.', 
    8, DATEADD('DAY', -14, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(23, 'Distributed Tracing with Zipkin', 
    'Implement distributed tracing across microservices using Spring Cloud Sleuth and Zipkin.', 
    8, DATEADD('DAY', -10, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(24, 'Structured Logging Best Practices', 
    'Improve log analysis with structured JSON logging, correlation IDs, and centralized log aggregation.', 
    8, DATEADD('DAY', -7, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- David New's posts (beginner friendly)
(25, 'Java 21 New Features Overview', 
    'Explore Java 21 LTS features including virtual threads, pattern matching, and record patterns.', 
    9, DATEADD('DAY', -5, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(26, 'Spring Boot Starter Guide', 
    'Complete beginner guide to Spring Boot covering auto-configuration, starters, and project structure.', 
    9, DATEADD('DAY', -3, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(27, 'Understanding Dependency Injection', 
    'Master Spring dependency injection with examples of constructor injection, setter injection, and field injection.', 
    9, DATEADD('DAY', -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

-- Emma Fresh's posts (recent content)
(28, 'GraphQL Subscriptions in Action', 
    'Implement real-time features with GraphQL subscriptions using WebSocket and Server-Sent Events.', 
    10, DATEADD('DAY', -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(29, 'Reactive Programming with Project Reactor', 
    'Introduction to reactive programming with Mono, Flux, and reactive operators for non-blocking applications.', 
    10, DATEADD('HOUR', -12, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),

(30, 'API Rate Limiting Strategies', 
    'Protect your APIs with rate limiting using bucket4j, Redis, and custom Spring interceptors.', 
    10, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP);

-- ===========================
-- DATA VERIFICATION QUERIES
-- ===========================
-- Use these queries to verify data loaded correctly:
-- SELECT COUNT(*) FROM users;      -- Should return 10
-- SELECT COUNT(*) FROM posts;      -- Should return 30
-- SELECT username, role FROM users ORDER BY created_at;
-- SELECT title, username FROM posts p JOIN users u ON p.user_id = u.id ORDER BY p.created_at DESC;

-- ===========================
-- TEST SCENARIOS SUPPORTED
-- ===========================
-- 1. Authentication: Login with any user (username/password: Password123!)
-- 2. Authorization: Test ADMIN, MODERATOR, USER roles
-- 3. Queries: Fetch users, posts, paginated results
-- 4. Mutations: Create, update, delete posts (as different users)
-- 5. Relationships: Query posts with authors, users with their posts
-- 6. Pagination: Test cursor-based pagination with 30 posts
-- 7. DataLoader: Query multiple users' posts to test N+1 prevention
-- 8. Caching: Repeated queries should hit Redis cache
-- 9. Error scenarios: Try unauthorized operations
-- 10. Subscriptions: Subscribe to new posts and create posts to test real-time updates
