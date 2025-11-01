# 🚀 Implementation Plan - Spring Boot GraphQL Enhancement

**Project**: SampleGraphQL  
**Plan Date**: November 1, 2025  
**Target Completion**: 8 weeks  
**Effort Estimation**: 200-250 hours

---

## Table of Contents
1. [Overview](#overview)
2. [Phase 1: Performance & Data Access](#phase-1-performance--data-access)
3. [Phase 2: Testing Infrastructure](#phase-2-testing-infrastructure)
4. [Phase 3: Security Hardening](#phase-3-security-hardening)
5. [Phase 4: Observability](#phase-4-observability)
6. [Phase 5: Resilience & Fault Tolerance](#phase-5-resilience--fault-tolerance)
7. [Phase 6: Production Readiness](#phase-6-production-readiness)
8. [Implementation Checklist](#implementation-checklist)

---

## Overview

This implementation plan addresses the critical gaps identified in the architectural review and provides a **step-by-step guide** to transform the application into a production-ready, enterprise-grade GraphQL service.

### Success Criteria
- ✅ 80%+ test coverage
- ✅ Sub-100ms P95 response time for simple queries
- ✅ Zero N+1 query issues
- ✅ Production-ready observability
- ✅ Comprehensive security controls
- ✅ Full CI/CD automation

---

## Phase 1: Performance & Data Access (Week 1-2)

### 1.1 Fix N+1 Query Problem

**Priority**: 🔴 CRITICAL  
**Effort**: 16 hours  
**Impact**: 10x performance improvement

#### Step 1: Implement DataLoader for User Entity

**File**: `src/main/java/com/example/graphql/dataloader/UserBatchLoader.java`

```java
package com.example.graphql.dataloader;

import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Batch loader for User entities to prevent N+1 query problem in GraphQL.
 * 
 * Instead of loading each user individually (N queries for N users),
 * this loader batches all user IDs and loads them in a single database query.
 * 
 * Performance Impact:
 * - Before: 1 + N queries (1 for posts, N for users)
 * - After: 2 queries (1 for posts, 1 for all users)
 * 
 * Example:
 * Query for 100 posts with their users:
 * - Without DataLoader: 101 database queries
 * - With DataLoader: 2 database queries
 */
@Component
public class UserBatchLoader {
    
    private final UserRepository userRepository;
    
    public UserBatchLoader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Creates a DataLoader for batching User entity loads.
     * 
     * The DataLoader will:
     * 1. Collect all requested user IDs
     * 2. Batch them together
     * 3. Execute a single query: SELECT * FROM users WHERE id IN (...)
     * 4. Map the results back to the original request order
     * 
     * @return Configured DataLoader for User entities
     */
    public DataLoader<Long, User> createUserDataLoader() {
        // Configure DataLoader options
        DataLoaderOptions options = DataLoaderOptions.newOptions()
            .setBatchingEnabled(true)           // Enable batching
            .setCachingEnabled(true)            // Cache results within request
            .setMaxBatchSize(100)               // Max IDs per batch
            .setBatchLoadFunction(this::batchLoadUsers);  // Batch function
            
        return DataLoaderFactory.newDataLoader(options);
    }
    
    /**
     * Batch load function that fetches multiple users in a single query.
     * 
     * @param userIds List of user IDs to load
     * @param environment DataLoader environment (for context)
     * @return CompletableFuture with list of users in the same order as userIds
     */
    private CompletableFuture<List<User>> batchLoadUsers(
            List<Long> userIds, 
            BatchLoaderEnvironment environment) {
        
        return Mono.fromCallable(() -> {
            // Single query for all user IDs
            List<User> users = userRepository.findAllById(userIds);
            
            // Create a map for O(1) lookup
            Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
            
            // Return users in the same order as requested IDs
            // This is critical for DataLoader to map results correctly
            return userIds.stream()
                .map(userMap::get)
                .collect(Collectors.toList());
        })
        .subscribeOn(Schedulers.boundedElastic())
        .toFuture();
    }
}
```

#### Step 2: Register DataLoader in GraphQL Context

**File**: `src/main/java/com/example/graphql/config/DataLoaderConfiguration.java`

```java
package com.example.graphql.config;

import com.example.graphql.dataloader.UserBatchLoader;
import graphql.GraphQLContext;
import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataLoaderRegistrar;

/**
 * Configuration for registering DataLoaders in the GraphQL context.
 * 
 * DataLoaders are registered per-request to ensure proper batching
 * and to avoid memory leaks from caching across requests.
 */
@Configuration
public class DataLoaderConfiguration implements DataLoaderRegistrar {
    
    public static final String USER_LOADER = "userLoader";
    
    private final UserBatchLoader userBatchLoader;
    
    public DataLoaderConfiguration(UserBatchLoader userBatchLoader) {
        this.userBatchLoader = userBatchLoader;
    }
    
    /**
     * Register all DataLoaders for each GraphQL request.
     * 
     * This method is called for every GraphQL request, creating
     * a fresh DataLoaderRegistry with new DataLoader instances.
     * 
     * @param registry Registry to add DataLoaders to
     * @param context GraphQL context for the current request
     */
    @Override
    public void registerDataLoaders(DataLoaderRegistry registry, GraphQLContext context) {
        registry.register(USER_LOADER, userBatchLoader.createUserDataLoader());
    }
}
```

#### Step 3: Use DataLoader in GraphQL Resolver

**File**: Update `src/main/java/com/example/graphql/controller/GraphQLController.java`

```java
/**
 * Field resolver for Post.user that uses DataLoader to prevent N+1 queries.
 * 
 * Instead of eagerly loading the user with the post, we use DataLoader
 * to batch all user loads into a single query.
 * 
 * @param post The post whose user we want to fetch
 * @param dataLoaderRegistry Registry containing all DataLoaders
 * @return CompletableFuture with the user
 */
@SchemaMapping(typeName = "Post", field = "user")
public CompletableFuture<User> postUser(
        Post post, 
        DataLoaderRegistry dataLoaderRegistry) {
    
    DataLoader<Long, User> userLoader = dataLoaderRegistry.getDataLoader(
        DataLoaderConfiguration.USER_LOADER
    );
    
    // This doesn't execute immediately - it's batched with other loads
    return userLoader.load(post.getUser().getId());
}
```

**Testing the Fix:**

```java
@Test
void testDataLoaderPreventsN1Queries() {
    // Enable SQL logging
    // Query for posts with users
    String query = """
        query {
            posts {
                id
                title
                user {
                    username
                    email
                }
            }
        }
    """;
    
    graphQlTester.document(query).execute();
    
    // Verify only 2 queries executed:
    // 1. SELECT * FROM posts
    // 2. SELECT * FROM users WHERE id IN (...)
    // Not 1 + N queries
}
```

---

### 1.2 Optimize Database Queries

**Priority**: 🔴 CRITICAL  
**Effort**: 12 hours

#### Step 1: Add JOIN FETCH Queries

**File**: Update `src/main/java/com/example/graphql/repository/PostRepository.java`

```java
package com.example.graphql.repository;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Post entity with optimized queries to prevent N+1 problems.
 * 
 * Key optimizations:
 * 1. JOIN FETCH for eager loading of associations
 * 2. @EntityGraph for alternative eager loading strategy
 * 3. Batch size configuration for collections
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    /**
     * Find all posts with users in a single query using JOIN FETCH.
     * 
     * This eliminates the N+1 query problem by eagerly loading users.
     * 
     * SQL Generated:
     * SELECT p.*, u.* 
     * FROM posts p 
     * INNER JOIN users u ON p.user_id = u.id
     * ORDER BY p.created_at DESC
     * 
     * Performance:
     * - Without JOIN FETCH: 1 + N queries
     * - With JOIN FETCH: 1 query
     */
    @Query("""
        SELECT p FROM Post p 
        JOIN FETCH p.user 
        ORDER BY p.createdAt DESC
    """)
    List<Post> findAllWithUsers();
    
    /**
     * Find post by ID with user eagerly loaded.
     * 
     * @param id Post ID
     * @return Optional with post and user, or empty
     */
    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :id")
    Optional<Post> findByIdWithUser(@Param("id") Long id);
    
    /**
     * Alternative to JOIN FETCH using @EntityGraph.
     * 
     * EntityGraph is more flexible and can be defined via annotations,
     * but JOIN FETCH gives more explicit control.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdWithEntityGraph(@Param("id") Long id);
    
    /**
     * Find posts by user with pagination.
     * JOIN FETCH cannot be used with pagination, so we use @EntityGraph.
     */
    @EntityGraph(attributePaths = {"user"})
    Page<Post> findByUser(User user, Pageable pageable);
    
    /**
     * Count posts by user efficiently.
     * This doesn't need JOIN FETCH since we're not accessing user data.
     */
    long countByUser(User user);
    
    /**
     * Search posts by title or content with user eagerly loaded.
     * 
     * @param search Search term
     * @return List of matching posts with users
     */
    @Query("""
        SELECT p FROM Post p 
        JOIN FETCH p.user 
        WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) 
        OR LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%'))
        ORDER BY p.createdAt DESC
    """)
    List<Post> searchPostsWithUsers(@Param("search") String search);
}
```

#### Step 2: Configure Batch Fetching

**File**: `src/main/java/com/example/graphql/entity/Post.java`

```java
/**
 * Configure batch fetching for the user association.
 * 
 * When multiple posts need their users loaded, Hibernate will
 * batch the user loads into a single query with IN clause.
 * 
 * Example:
 * Instead of: SELECT * FROM users WHERE id = ? (executed N times)
 * Execute: SELECT * FROM users WHERE id IN (?, ?, ?, ..., ?)
 */
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "user_id", nullable = false)
@BatchSize(size = 25)  // Batch size for loading users
private User user;
```

---

### 1.3 Configure HikariCP Connection Pool

**Priority**: 🟡 HIGH  
**Effort**: 4 hours

**File**: Update `src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    # HikariCP is the default in Spring Boot, but we'll configure it explicitly
    hikari:
      # Pool sizing
      minimum-idle: 5                    # Minimum number of idle connections
      maximum-pool-size: 20              # Maximum pool size
      
      # Connection lifecycle
      connection-timeout: 30000          # 30s - Max time to wait for connection
      idle-timeout: 600000               # 10min - Max idle time before eviction
      max-lifetime: 1800000              # 30min - Max lifetime of connection
      
      # Connection testing
      connection-test-query: SELECT 1    # Query to test connection validity
      validation-timeout: 5000           # 5s - Max time for validation query
      
      # Pool management
      pool-name: GraphQLHikariPool
      auto-commit: true
      leak-detection-threshold: 60000    # 60s - Detect connection leaks
      
      # Performance optimizations
      data-source-properties:
        cachePrepStmts: true             # Enable prepared statement caching
        prepStmtCacheSize: 250           # Number of cached statements
        prepStmtCacheSqlLimit: 2048      # Max length of cached statement
        useServerPrepStmts: true         # Use server-side prepared statements
        
  jpa:
    properties:
      hibernate:
        # JDBC batching for inserts/updates
        jdbc:
          batch_size: 25                 # Batch size for bulk operations
          batch_versioned_data: true
        order_inserts: true              # Order inserts for better batching
        order_updates: true              # Order updates for better batching
        
        # Query optimization
        query:
          in_clause_parameter_padding: true  # Optimize IN clause
          fail_on_pagination_over_collection_fetch: true  # Prevent pagination bugs
```

**For Production** (`application-prod.yml`):

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50              # Higher for production load
      connection-timeout: 20000          # Faster timeout
      idle-timeout: 300000               # 5min - More aggressive eviction
      leak-detection-threshold: 30000    # 30s - Earlier leak detection
```

**For 8GB Laptop** (`application-local.yml`):

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 2                    # Minimal idle connections
      maximum-pool-size: 10              # Conservative pool size
      connection-timeout: 30000
      idle-timeout: 600000
```

---

### 1.4 Implement Cursor-Based Pagination

**Priority**: 🟡 HIGH  
**Effort**: 8 hours

**File**: `src/main/java/com/example/graphql/dto/PageInput.java`

```java
package com.example.graphql.dto;

/**
 * Input for cursor-based pagination following Relay specification.
 * 
 * Cursor-based pagination is preferred over offset pagination for:
 * 1. Consistency - No skipped/duplicate items when data changes
 * 2. Performance - Uses indexed cursors instead of OFFSET
 * 3. Real-time - Works well with live data updates
 * 
 * Usage:
 * - first + after: Get first N items after cursor
 * - last + before: Get last N items before cursor
 */
public record PageInput(
    Integer first,      // Number of items to return from start
    String after,       // Cursor to start after
    Integer last,       // Number of items to return from end
    String before       // Cursor to end before
) {
    public PageInput {
        // Validation
        if (first != null && first < 0) {
            throw new IllegalArgumentException("first must be non-negative");
        }
        if (last != null && last < 0) {
            throw new IllegalArgumentException("last must be non-negative");
        }
        if (first != null && last != null) {
            throw new IllegalArgumentException("Cannot use both first and last");
        }
        if (first == null && last == null) {
            first = 10; // Default page size
        }
    }
}
```

**File**: `src/main/java/com/example/graphql/dto/Connection.java`

```java
package com.example.graphql.dto;

import java.util.List;

/**
 * Connection type for Relay-style pagination.
 * 
 * A connection contains:
 * - edges: List of items with cursors
 * - pageInfo: Pagination metadata
 * - totalCount: Total items available (optional, can be expensive)
 */
public record Connection<T>(
    List<Edge<T>> edges,
    PageInfo pageInfo,
    Integer totalCount
) {
    public static <T> Connection<T> empty() {
        return new Connection<>(
            List.of(),
            new PageInfo(false, false, null, null),
            0
        );
    }
}
```

**File**: `src/main/java/com/example/graphql/dto/Edge.java`

```java
package com.example.graphql.dto;

/**
 * Edge in a connection, pairing an item with its cursor.
 */
public record Edge<T>(
    String cursor,  // Opaque cursor for this item
    T node          // The actual item
) {}
```

**File**: `src/main/java/com/example/graphql/dto/PageInfo.java`

```java
package com.example.graphql.dto;

/**
 * Metadata about the current page in a connection.
 */
public record PageInfo(
    boolean hasNextPage,      // More items available after this page
    boolean hasPreviousPage,  // More items available before this page
    String startCursor,       // Cursor of first item in page
    String endCursor          // Cursor of last item in page
) {}
```

**File**: `src/main/java/com/example/graphql/service/PaginationService.java`

```java
package com.example.graphql.service;

import com.example.graphql.dto.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Service for implementing cursor-based pagination.
 * 
 * Cursors are base64-encoded IDs for opacity and security.
 */
@Service
public class PaginationService {
    
    /**
     * Create a connection from a list of items.
     * 
     * @param items All items (will be sliced for pagination)
     * @param pageInput Pagination parameters
     * @param idExtractor Function to extract ID from item (for cursor)
     * @return Connection with paginated items
     */
    public <T> Connection<T> createConnection(
            List<T> items,
            PageInput pageInput,
            Function<T, Long> idExtractor) {
        
        if (items.isEmpty()) {
            return Connection.empty();
        }
        
        // Determine slice of items to return
        int start = 0;
        int end = items.size();
        
        if (pageInput.after() != null) {
            Long afterId = decodeCursor(pageInput.after());
            start = findIndexAfter(items, afterId, idExtractor);
        }
        
        if (pageInput.before() != null) {
            Long beforeId = decodeCursor(pageInput.before());
            end = findIndexBefore(items, beforeId, idExtractor);
        }
        
        if (pageInput.first() != null) {
            end = Math.min(end, start + pageInput.first());
        }
        
        if (pageInput.last() != null) {
            start = Math.max(start, end - pageInput.last());
        }
        
        // Create edges
        List<Edge<T>> edges = IntStream.range(start, end)
            .mapToObj(i -> {
                T item = items.get(i);
                String cursor = encodeCursor(idExtractor.apply(item));
                return new Edge<>(cursor, item);
            })
            .toList();
        
        // Create page info
        PageInfo pageInfo = new PageInfo(
            end < items.size(),                          // hasNextPage
            start > 0,                                    // hasPreviousPage
            edges.isEmpty() ? null : edges.get(0).cursor(),           // startCursor
            edges.isEmpty() ? null : edges.get(edges.size() - 1).cursor()  // endCursor
        );
        
        return new Connection<>(edges, pageInfo, items.size());
    }
    
    /**
     * Encode an ID as a base64 cursor.
     */
    public String encodeCursor(Long id) {
        String cursorString = "cursor:" + id;
        return Base64.getEncoder()
            .encodeToString(cursorString.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Decode a base64 cursor to an ID.
     */
    public Long decodeCursor(String cursor) {
        String decoded = new String(
            Base64.getDecoder().decode(cursor),
            StandardCharsets.UTF_8
        );
        return Long.parseLong(decoded.substring("cursor:".length()));
    }
    
    private <T> int findIndexAfter(List<T> items, Long afterId, Function<T, Long> idExtractor) {
        for (int i = 0; i < items.size(); i++) {
            if (idExtractor.apply(items.get(i)).equals(afterId)) {
                return i + 1;
            }
        }
        return 0;
    }
    
    private <T> int findIndexBefore(List<T> items, Long beforeId, Function<T, Long> idExtractor) {
        for (int i = 0; i < items.size(); i++) {
            if (idExtractor.apply(items.get(i)).equals(beforeId)) {
                return i;
            }
        }
        return items.size();
    }
}
```

**Update GraphQL Schema** (`src/main/resources/schema.graphqls`):

```graphql
# Add to schema
type Query {
    # ... existing queries ...
    
    # Paginated queries
    postsConnection(input: PageInput): PostConnection!
    usersConnection(input: PageInput): UserConnection!
}

input PageInput {
    first: Int
    after: String
    last: Int
    before: String
}

type PostConnection {
    edges: [PostEdge!]!
    pageInfo: PageInfo!
    totalCount: Int!
}

type PostEdge {
    cursor: String!
    node: Post!
}

type UserConnection {
    edges: [UserEdge!]!
    pageInfo: PageInfo!
    totalCount: Int!
}

type UserEdge {
    cursor: String!
    node: User!
}

type PageInfo {
    hasNextPage: Boolean!
    hasPreviousPage: Boolean!
    startCursor: String
    endCursor: String
}
```

**Add Resolver** in `GraphQLController.java`:

```java
@QueryMapping
public Connection<Post> postsConnection(@Argument PageInput input) {
    List<Post> allPosts = postService.findAllWithUsers();
    return paginationService.createConnection(allPosts, input, Post::getId);
}
```

---

## Phase 2: Testing Infrastructure (Week 3-4)

### 2.1 Unit Testing Strategy

**Priority**: 🔴 CRITICAL  
**Effort**: 24 hours  
**Target Coverage**: 80%

#### Test Structure

```
src/test/java/
├── com/example/graphql/
│   ├── unit/
│   │   ├── service/
│   │   │   ├── UserServiceTest.java
│   │   │   ├── PostServiceTest.java
│   │   │   └── PaginationServiceTest.java
│   │   ├── security/
│   │   │   ├── JwtUtilTest.java
│   │   │   └── JwtWebFilterTest.java
│   │   └── dataloader/
│   │       └── UserBatchLoaderTest.java
│   ├── integration/
│   │   ├── GraphQLQueryTest.java
│   │   ├── GraphQLMutationTest.java
│   │   ├── GraphQLSubscriptionTest.java
│   │   └── SecurityIntegrationTest.java
│   └── performance/
│       ├── N1QueryTest.java
│       └── LoadTest.java
```

#### Example Unit Test

**File**: `src/test/java/com/example/graphql/unit/service/UserServiceTest.java`

```java
package com.example.graphql.unit.service;

import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import com.example.graphql.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * 
 * Testing Strategy:
 * 1. Mock all dependencies (repository, password encoder)
 * 2. Test each method in isolation
 * 3. Verify interactions with mocks
 * 4. Test both success and failure scenarios
 * 5. Use StepVerifier for reactive streams
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User(
            "testuser",
            "test@example.com",
            "encodedPassword",
            User.Role.USER
        );
    }
    
    @Test
    @DisplayName("findByUsername should return user when found")
    void findByUsername_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findByUsername("testuser"))
            .thenReturn(Optional.of(testUser));
        
        // When
        Mono<User> result = userService.findByUsernameReactive("testuser");
        
        // Then
        StepVerifier.create(result)
            .assertNext(user -> {
                assertThat(user.getUsername()).isEqualTo("testuser");
                assertThat(user.getEmail()).isEqualTo("test@example.com");
            })
            .verifyComplete();
        
        verify(userRepository, times(1)).findByUsername("testuser");
    }
    
    @Test
    @DisplayName("findByUsername should return empty when user not found")
    void findByUsername_ShouldReturnEmpty_WhenUserNotFound() {
        // Given
        when(userRepository.findByUsername(anyString()))
            .thenReturn(Optional.empty());
        
        // When
        Mono<User> result = userService.findByUsernameReactive("nonexistent");
        
        // Then
        StepVerifier.create(result)
            .verifyComplete();  // Empty mono
    }
    
    @Test
    @DisplayName("createUser should encode password and save user")
    void createUser_ShouldEncodePasswordAndSave() {
        // Given
        String rawPassword = "password123";
        String encodedPassword = "encoded_password";
        
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        Mono<User> result = userService.createUserReactive(
            "newuser",
            "new@example.com",
            rawPassword,
            "USER"
        );
        
        // Then
        StepVerifier.create(result)
            .assertNext(user -> {
                assertThat(user).isNotNull();
            })
            .verifyComplete();
        
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    @DisplayName("createUser should throw exception for invalid role")
    void createUser_ShouldThrowException_ForInvalidRole() {
        // When/Then
        Mono<User> result = userService.createUserReactive(
            "newuser",
            "new@example.com",
            "password",
            "INVALID_ROLE"
        );
        
        StepVerifier.create(result)
            .expectError(IllegalArgumentException.class)
            .verify();
        
        verify(userRepository, never()).save(any());
    }
}
```

### 2.2 Integration Testing

**File**: `src/test/java/com/example/graphql/integration/GraphQLQueryTest.java`

```java
package com.example.graphql.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.assertions.Assertj.assertThat;

/**
 * Integration tests for GraphQL queries.
 * 
 * These tests:
 * 1. Start the full Spring context
 * 2. Use real database (H2 in-memory)
 * 3. Execute actual GraphQL queries
 * 4. Verify responses and data
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@ActiveProfiles("test")
@DisplayName("GraphQL Query Integration Tests")
class GraphQLQueryTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Test
    @DisplayName("Query all users should return list")
    void queryAllUsers_ShouldReturnList() {
        String query = """
            query {
                users {
                    id
                    username
                    email
                    role
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .execute()
            .path("users")
            .entityList(Object.class)
            .hasSizeGreaterThan(0);
    }
    
    @Test
    @DisplayName("Query posts with pagination should work")
    void queryPostsWithPagination_ShouldWork() {
        String query = """
            query {
                postsConnection(input: { first: 5 }) {
                    edges {
                        cursor
                        node {
                            id
                            title
                        }
                    }
                    pageInfo {
                        hasNextPage
                        hasPreviousPage
                    }
                    totalCount
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .execute()
            .path("postsConnection.edges")
            .entityList(Object.class)
            .hasSize(5)
            .path("postsConnection.pageInfo.hasNextPage")
            .entity(Boolean.class)
            .satisfies(hasNext -> {
                // Should have next page if more than 5 posts exist
            });
    }
}
```

---

## Phase 3: Security Hardening (Week 5)

### 3.1 Input Validation

**File**: `src/main/java/com/example/graphql/validation/GraphQLInputValidator.java`

```java
package com.example.graphql.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for GraphQL input objects.
 * 
 * Provides centralized validation with descriptive error messages.
 */
@Component
public class GraphQLInputValidator {
    
    private final Validator validator;
    
    public GraphQLInputValidator(Validator validator) {
        this.validator = validator;
    }
    
    /**
     * Validate an input object and throw exception if invalid.
     * 
     * @param input Input object to validate
     * @throws ValidationException if validation fails
     */
    public <T> void validate(T input) {
        Set<ConstraintViolation<T>> violations = validator.validate(input);
        
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
            
            throw new ValidationException("Input validation failed: " + errors);
        }
    }
}
```

**File**: `src/main/java/com/example/graphql/dto/CreateUserInput.java`

```java
package com.example.graphql.dto;

import jakarta.validation.constraints.*;

/**
 * Input for creating a new user with validation.
 */
public record CreateUserInput(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    String username,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one number"
    )
    String password,
    
    @NotNull(message = "Role is required")
    String role
) {}
```

---

*Continue with remaining phases...*

---

## Implementation Checklist

### Phase 1: Performance ✅
- [ ] Implement DataLoader for User entities
- [ ] Add JOIN FETCH queries to repositories
- [ ] Configure HikariCP connection pooling
- [ ] Implement cursor-based pagination
- [ ] Add batch fetching configuration
- [ ] Test N+1 query fixes

### Phase 2: Testing ✅
- [ ] Write unit tests for services (80% coverage)
- [ ] Write integration tests for GraphQL queries
- [ ] Write integration tests for mutations
- [ ] Add security tests
- [ ] Add performance tests
- [ ] Configure test coverage reporting

### Phase 3: Security ✅
- [ ] Add input validation for all mutations
- [ ] Implement method-level security
- [ ] Add query complexity enforcement
- [ ] Implement rate limiting per user
- [ ] Add audit logging
- [ ] Externalize secrets management

### Phase 4: Observability ✅
- [ ] Add distributed tracing
- [ ] Implement custom metrics
- [ ] Add structured logging
- [ ] Create health indicators
- [ ] Set up monitoring dashboards

### Phase 5: Resilience ✅
- [ ] Implement bulkhead pattern
- [ ] Add timeout configurations
- [ ] Enhance fallback strategies
- [ ] Add distributed locking
- [ ] Test circuit breaker behavior

### Phase 6: Production ✅
- [ ] Optimize Docker image
- [ ] Create Kubernetes manifests
- [ ] Set up CI/CD pipeline
- [ ] Create environment configs
- [ ] Write deployment documentation

---

**Total Estimated Effort**: 200-250 hours over 8 weeks

*This plan should be adjusted based on team capacity and business priorities.*
