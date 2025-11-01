# 🚀 Spring Boot GraphQL - Production-Ready Example

A comprehensive, production-grade Spring Boot 3 application showcasing GraphQL best practices, designed for both beginners and advanced developers. Features JWT authentication, reactive programming, caching, resilience patterns, and complete observability.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![GraphQL](https://img.shields.io/badge/GraphQL-Spring-E10098.svg)](https://spring.io/projects/spring-graphql)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **🎯 Learning Resource**: This project serves as a complete reference implementation with extensive documentation, testing examples, and deployment guides.

---

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Documentation](#documentation)
- [Testing](#testing)
- [Deployment](#deployment)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)

---

## ✨ Features

### Core Capabilities
- ✅ **GraphQL API** - Schema-first design with queries, mutations, and subscriptions
- ✅ **JWT Authentication** - Stateless auth with access & refresh tokens
- ✅ **Reactive Programming** - Spring WebFlux for non-blocking I/O
- ✅ **Redis Caching** - Distributed caching for performance
- ✅ **Resilience Patterns** - Circuit breaker, retry, rate limiting
- ✅ **Database Support** - H2 (dev) and PostgreSQL (production)
- ✅ **Real-time Updates** - GraphQL subscriptions via WebSocket
- ✅ **Input Validation** - Bean validation on all inputs
- ✅ **Error Handling** - Comprehensive exception handling
- ✅ **Security** - CORS, CSRF protection, role-based access

### Advanced Features
- ✅ **DataLoader Pattern** - Prevents N+1 query problems
- ✅ **Query Complexity Limits** - Protection against malicious queries
- ✅ **Observability** - Metrics, logging, tracing with Prometheus/Grafana
- ✅ **Health Checks** - Kubernetes-ready liveness/readiness probes
- ✅ **API Documentation** - Self-documenting with GraphiQL playground
- ✅ **Environment Profiles** - Separate configs for local/dev/qa/prod

---

## 🏗️ Architecture


```
┌─────────────┐
│   Client    │ (Web, Mobile, Postman)
└──────┬──────┘
       │ HTTP/WebSocket
       ↓
┌──────────────────────────────────────┐
│      GraphQL API Layer               │
│  ┌────────────────────────────────┐  │
│  │  GraphQL Controller            │  │
│  │  - Query/Mutation/Subscription │  │
│  │  - Input Validation            │  │
│  │  - Rate Limiting               │  │
│  └────────────────────────────────┘  │
└──────┬────────────┬──────────────────┘
       │            │
       ↓            ↓
┌──────────────┐ ┌──────────────┐
│ JWT Security │ │ DataLoader   │
│ - Auth Filter│ │ - Batching   │
│ - Token Valid│ │ - Caching    │
└──────┬───────┘ └──────┬───────┘
       │                │
       ↓                ↓
┌─────────────────────────────────────┐
│      Business Layer                 │
│  ┌──────────────┐  ┌─────────────┐  │
│  │ User Service │  │ Post Service│  │
│  │ - Cache      │  │ - Resilience│  │
│  │ - Validation │  │ - External  │  │
│  └──────┬───────┘  └──────┬──────┘  │
└─────────┼──────────────────┼─────────┘
          │                  │
          ↓                  ↓
┌─────────────────┐  ┌──────────────┐
│   PostgreSQL    │  │    Redis     │
│   (Production)  │  │   (Cache)    │
│   H2 (Dev)      │  │              │
└─────────────────┘  └──────────────┘
```

### Key Components
- **API Layer**: GraphQL schema, controllers, input validation
- **Security**: JWT authentication, role-based authorization
- **Business Layer**: Services with caching, resilience patterns
- **Data Layer**: JPA repositories, Redis caching
- **Infrastructure**: Prometheus metrics, health checks, logging

---

## 🚀 Quick Start

### Prerequisites
- **Java 21** (LTS)
- **Gradle 8+**
- **Redis** (for caching)
- **Docker** (optional, for containers)
- **8GB RAM** minimum

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/SrineelM/SampleGraphQL.git
cd SampleGraphQL

# 2. Start Redis (choose one method)
# Option A: Docker
docker run -d -p 6379:6379 redis:7-alpine

# Option B: Local installation
brew install redis  # macOS
redis-server

# 3. Run the application
./gradlew bootRun

# 4. Application will start on http://localhost:8080
```

### First Request

Open GraphiQL at http://localhost:8080/graphiql

```graphql
# Register a new user
mutation {
  register(input: {
    username: "john_doe"
    email: "john@example.com"
    password: "SecurePass123!"
  }) {
    token
    user {
      id
      username
      email
    }
  }
}
```

Copy the `token` from the response, then add it as a header:
```
Authorization: Bearer <your-token>
```

Now query your profile:
```graphql
query {
  me {
    id
    username
    email
    posts {
      title
    }
  }
}
```

---

## 📚 Documentation

Comprehensive guides available in the `docs/` directory:

| Guide | Description | Audience |
|-------|-------------|----------|
| [**Architectural Review**](docs/ARCHITECTURAL_REVIEW.md) | In-depth architecture analysis with strengths, gaps, and recommendations | Architects, Senior Developers |
| [**Implementation Plan**](docs/IMPLEMENTATION_PLAN.md) | Step-by-step enhancement guide with code examples | Developers |
| [**Testing Strategy**](docs/TESTING_STRATEGY.md) | Unit, integration, and performance testing examples | QA, Developers |
| [**Postman Testing Guide**](docs/POSTMAN_TESTING_GUIDE.md) | API testing with sample requests and collection | API Testers |
| [**Configuration Guide**](docs/CONFIGURATION_GUIDE.md) | Environment-specific configurations (local/dev/qa/prod) | DevOps, Developers |
| [**GraphQL vs REST**](docs/GRAPHQL_VS_REST.md) | Decision framework for choosing between GraphQL and REST | Architects |
| [**Security Checklist**](docs/SECURITY_CHECKLIST.md) | Security best practices and implementation | Security Engineers |
| [**Observability Guide**](docs/OBSERVABILITY_GUIDE.md) | Monitoring, metrics, logging, and tracing | DevOps, SRE |
| [**Deployment Guide**](docs/DEPLOYMENT_GUIDE.md) | Docker, Kubernetes, CI/CD deployment strategies | DevOps |

### Quick References

**For Beginners:**
1. Start with [Quick Start](#quick-start)
2. Read [Postman Testing Guide](docs/POSTMAN_TESTING_GUIDE.md)
3. Explore [Configuration Guide](docs/CONFIGURATION_GUIDE.md)

**For Advanced Users:**
1. Review [Architectural Review](docs/ARCHITECTURAL_REVIEW.md)
2. Follow [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
3. Implement [Security Checklist](docs/SECURITY_CHECKLIST.md)

---

## 🧪 Testing

### Run All Tests
```bash
./gradlew test
```

### Run with Coverage
```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

### Test Categories

**Unit Tests** (70% of tests)
```bash
./gradlew test --tests "*Test"
```

**Integration Tests** (20% of tests)
```bash
./gradlew test --tests "*IntegrationTest"
```

**GraphQL API Tests**
```java
@SpringBootTest
@AutoConfigureHttpGraphQlTester
class GraphQLControllerTest {
    @Autowired
    HttpGraphQlTester graphQlTester;
    
    @Test
    void shouldQueryUser() {
        graphQlTester.document("""
            query { user(id: 1) { username } }
        """)
        .execute()
        .path("user.username").entity(String.class).isEqualTo("john");
    }
}
```

See [Testing Strategy](docs/TESTING_STRATEGY.md) for complete examples.

---

## 🚢 Deployment

### Docker Deployment
```bash
# Build image
docker build -t graphql-api:latest .

# Run with Docker Compose (includes PostgreSQL and Redis)
docker-compose up -d

# Verify
curl http://localhost:8080/actuator/health
```

### Kubernetes Deployment
```bash
# Apply manifests
kubectl apply -f k8s/

# Check status
kubectl get pods
kubectl logs -f deployment/graphql-api
```

### Production Checklist
- [ ] Environment variables configured (see [Configuration Guide](docs/CONFIGURATION_GUIDE.md))
- [ ] Secrets stored in vault/secrets manager
- [ ] Database migrations applied
- [ ] Health checks responding
- [ ] Monitoring and alerting configured
- [ ] Load testing completed
- [ ] SSL/TLS enabled

See [Deployment Guide](docs/DEPLOYMENT_GUIDE.md) for detailed instructions.

---

## 📁 Project Structure


```
src/main/
├── java/com/example/graphql/
│   ├── GraphQLPocApplication.java          # Main application entry point
│   ├── api/
│   │   └── ApiError.java                   # Error response models
│   ├── config/
│   │   ├── GraphQLMetricsConfig.java       # Metrics configuration
│   │   ├── GraphQLRateLimitConfig.java     # Rate limiting setup
│   │   └── ReactiveConfig.java             # Reactive programming config
│   ├── controller/
│   │   ├── AuthController.java             # REST auth endpoints
│   │   ├── GraphQLController.java          # GraphQL resolvers
│   │   └── PostSubscriptionController.java # Subscription handlers
│   ├── dataloader/
│   │   └── UserDataLoader.java             # DataLoader for batching
│   ├── dto/
│   │   ├── AuthPayload.java                # Authentication responses
│   │   ├── LoginInput.java                 # Login request DTO
│   │   ├── UserInput.java                  # User creation DTO
│   │   └── UserUpdateDTO.java              # User update DTO
│   ├── entity/
│   │   ├── User.java                       # User entity (JPA)
│   │   ├── Post.java                       # Post entity (JPA)
│   │   └── ...
│   ├── exception/
│   │   ├── CustomGraphQLException.java     # Custom exceptions
│   │   └── GraphQlExceptionHandler.java    # Global error handler
│   ├── interceptor/
│   │   └── GraphQLRateLimitInterceptor.java # Rate limiting
│   ├── publisher/
│   │   └── PostPublisher.java              # WebSocket publisher
│   ├── repository/
│   │   ├── UserRepository.java             # User data access
│   │   └── PostRepository.java             # Post data access
│   ├── scalar/
│   │   └── DateTimeScalar.java             # Custom GraphQL scalar
│   ├── security/
│   │   ├── CustomUserDetailsService.java   # User details service
│   │   ├── JwtUtil.java                    # JWT token utilities
│   │   ├── JwtWebFilter.java               # JWT authentication filter
│   │   └── SecurityConfig.java             # Security configuration
│   └── service/
│       ├── ExternalServiceClient.java      # External API client
│       ├── PostService.java                # Post business logic
│       └── UserService.java                # User business logic
│
├── resources/
│   ├── schema.graphqls                     # GraphQL schema definition
│   ├── application.yml                     # Base configuration
│   ├── application-local.yml               # Local dev config (8GB laptop)
│   ├── application-dev.yml                 # Development config
│   ├── application-qa.yml                  # QA config
│   ├── application-prod.yml                # Production config
│   ├── data.sql                            # Sample test data
│   └── schema.sql                          # Database schema
│
├── test/
│   └── java/com/example/graphql/
│       ├── GraphQLIntegrationTest.java     # Integration tests
│       ├── GraphQLPocApplicationTest.java  # Application tests
│       ├── controller/
│       │   └── GraphQLControllerTest.java  # Controller unit tests
│       ├── service/
│       │   ├── UserServiceTest.java        # Service unit tests
│       │   └── PostServiceTest.java
│       └── security/
│           └── JwtUtilTest.java            # Security tests
│
├── docs/                                    # Comprehensive documentation
│   ├── ARCHITECTURAL_REVIEW.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── TESTING_STRATEGY.md
│   ├── POSTMAN_TESTING_GUIDE.md
│   ├── CONFIGURATION_GUIDE.md
│   ├── GRAPHQL_VS_REST.md
│   ├── SECURITY_CHECKLIST.md
│   ├── OBSERVABILITY_GUIDE.md
│   └── DEPLOYMENT_GUIDE.md
│
├── build.gradle                             # Build configuration
├── lombok.config                            # Lombok settings
├── guideline.md                             # Development guidelines
└── README.md                                # This file
```

---

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot 3.2.4** - Application framework
- **Spring WebFlux** - Reactive web framework
- **Spring GraphQL 1.2.5** - GraphQL implementation
- **Spring Security 6.x** - Authentication & authorization
- **Spring Data JPA** - Data access layer

### Database & Caching
- **H2 Database** - In-memory database for development
- **PostgreSQL 16** - Production database
- **Redis 7** - Distributed caching & session storage
- **HikariCP** - High-performance connection pooling

### Security
- **JWT (jjwt 0.12.5)** - Stateless authentication
- **BCrypt** - Password hashing
- **Spring Security** - Authorization framework

### Resilience & Monitoring
- **Resilience4j** - Circuit breaker, retry, rate limiting
- **Micrometer** - Application metrics
- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization
- **Spring Boot Actuator** - Health checks, metrics endpoints

### Testing
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **Spring GraphQL Test** - GraphQL testing utilities
- **Testcontainers** - Integration testing with containers
- **JaCoCo** - Code coverage

### Build & DevOps
- **Gradle 8+** - Build automation
- **Docker** - Containerization
- **Kubernetes** - Container orchestration
- **GitHub Actions** - CI/CD pipeline

---

## 🔐 Security

### Authentication Flow
1. User registers/logs in via `register` or `login` mutation
2. Server returns access token (15 min) + refresh token (7 days)
3. Client includes token in `Authorization: Bearer <token>` header
4. Server validates token on each request
5. Refresh token used to obtain new access token

### Security Features
- ✅ JWT-based stateless authentication
- ✅ BCrypt password hashing (cost factor 12)
- ✅ Role-based access control (USER, ADMIN, MODERATOR)
- ✅ Method-level security with `@PreAuthorize`
- ✅ Query complexity and depth limits
- ✅ Rate limiting (per-user and global)
- ✅ Input validation on all mutations
- ✅ CORS configuration
- ✅ Secure error handling (no info leakage)

See [Security Checklist](docs/SECURITY_CHECKLIST.md) for complete details.

---

## 📊 API Examples

### Authentication
```graphql
# Register new user
mutation {
  register(input: {
    username: "alice"
    email: "alice@example.com"
    password: "SecurePass123!"
  }) {
    token
    refreshToken
    user { id username role }
  }
}

# Login
mutation {
  login(input: {
    username: "alice"
    password: "SecurePass123!"
  }) {
    token
    refreshToken
  }
}
```

### Queries
```graphql
# Get current user
query {
  me {
    id
    username
    email
    posts {
      id
      title
      createdAt
    }
  }
}

# Get all users with pagination
query {
  users(first: 10, after: "cursor") {
    edges {
      node {
        id
        username
      }
      cursor
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

### Mutations
```graphql
# Create post
mutation {
  createPost(input: {
    title: "GraphQL Best Practices"
    content: "..."
  }) {
    id
    title
    author {
      username
    }
  }
}

# Update user
mutation {
  updateUser(input: {
    id: 1
    email: "newemail@example.com"
  }) {
    id
    email
  }
}
```

### Subscriptions
```graphql
# Subscribe to new posts
subscription {
  postAdded {
    id
    title
    author {
      username
    }
  }
}
```

---

## 🔧 Configuration

### Environment Profiles

| Profile | Database | Cache | Use Case |
|---------|----------|-------|----------|
| **local** | H2 in-memory | Simple cache | 8GB laptop development |
| **dev** | PostgreSQL | Redis | Development environment |
| **qa** | PostgreSQL | Redis Cluster | QA testing |
| **prod** | PostgreSQL HA | Redis Cluster | Production |

### Running with Specific Profile
```bash
# Local (optimized for 8GB laptop)
./gradlew bootRun --args='--spring.profiles.active=local'

# Development
./gradlew bootRun --args='--spring.profiles.active=dev'

# Production
java -jar -Dspring.profiles.active=prod build/libs/graphql-poc.jar
```

See [Configuration Guide](docs/CONFIGURATION_GUIDE.md) for detailed configuration options.

---

## 🎯 Key Endpoints

| Endpoint | Purpose | Method |
|----------|---------|--------|
| `/graphql` | GraphQL API | POST |
| `/graphiql` | GraphiQL Playground (dev only) | GET |
| `/graphql` (WebSocket) | Subscriptions | WS |
| `/actuator/health` | Health check | GET |
| `/actuator/metrics` | Metrics | GET |
| `/actuator/prometheus` | Prometheus metrics | GET |
| `/h2-console` | H2 database console (dev only) | GET |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📝 Best Practices Implemented

- ✅ **Schema-First Design** - GraphQL schema drives development
- ✅ **DataLoader Pattern** - Prevents N+1 queries
- ✅ **Cursor-Based Pagination** - Relay specification compliant
- ✅ **Reactive Programming** - Non-blocking I/O with Project Reactor
- ✅ **Caching Strategy** - Multi-level caching (local + distributed)
- ✅ **Circuit Breaker** - Resilience against external service failures
- ✅ **Rate Limiting** - Protection against abuse
- ✅ **Input Validation** - Bean validation on all DTOs
- ✅ **Structured Logging** - JSON logging for centralized aggregation
- ✅ **Health Checks** - Kubernetes-ready probes
- ✅ **Metrics Collection** - Prometheus-compatible metrics
- ✅ **Security** - Defense in depth approach

---

## 🐛 Troubleshooting

### Redis Connection Failed
```bash
# Check if Redis is running
redis-cli ping
# Expected: PONG

# Start Redis if not running
docker run -d -p 6379:6379 redis:7-alpine
```

### Database Connection Error
```yaml
# For local development, H2 is used (no setup needed)
# Check application-local.yml is active
spring:
  profiles:
    active: local
```

### JWT Token Invalid
```bash
# Ensure JWT secret is set (minimum 32 characters)
export JWT_SECRET=$(openssl rand -base64 32)
```

---

## 📖 Additional Resources

- [Spring GraphQL Documentation](https://docs.spring.io/spring-graphql/reference/)
- [GraphQL Best Practices](https://graphql.org/learn/best-practices/)
- [Spring Security](https://spring.io/projects/spring-security)
- [Resilience4j Guide](https://resilience4j.readme.io/)
- [Project Reactor](https://projectreactor.io/docs)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **Srineel M** - [GitHub](https://github.com/SrineelM)

---

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- GraphQL community for best practices
- All contributors to the open-source libraries used

---

**⭐ If you find this project helpful, please give it a star!**

*This project is designed as a comprehensive learning resource for Spring Boot and GraphQL. Explore the documentation for in-depth guides and examples.*

