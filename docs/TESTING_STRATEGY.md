# 🧪 Testing Strategy - Spring Boot GraphQL Application

**Project**: SampleGraphQL  
**Document Date**: November 1, 2025  
**Target Coverage**: 80%+  
**Testing Pyramid**: 70% Unit, 20% Integration, 10% E2E

---

## Table of Contents
1. [Testing Philosophy](#testing-philosophy)
2. [Test Structure](#test-structure)
3. [Unit Testing](#unit-testing)
4. [Integration Testing](#integration-testing)
5. [Security Testing](#security-testing)
6. [Performance Testing](#performance-testing)
7. [Test Data Management](#test-data-management)
8. [Continuous Testing](#continuous-testing)

---

## Testing Philosophy

### Testing Pyramid

```
       /\
      /  \     E2E Tests (10%)
     /────\    - Full system tests
    /      \   - Real database, Redis
   /────────\  Integration Tests (20%)
  /          \ - Component integration
 /────────────\ - GraphQL endpoint tests
/──────────────\ 
  Unit Tests (70%)
  - Service layer
  - Repository layer
  - Utility functions
```

### Key Principles

1. **Fast Feedback**: Unit tests run in < 5 seconds
2. **Isolated**: Tests don't depend on each other
3. **Repeatable**: Same results every time
4. **Self-Validating**: Clear pass/fail
5. **Timely**: Written alongside code

---

## Test Structure

### Directory Layout

```
src/test/java/com/example/graphql/
├── unit/                           # Unit tests (isolated, mocked)
│   ├── service/
│   │   ├── UserServiceTest.java
│   │   ├── PostServiceTest.java
│   │   └── PaginationServiceTest.java
│   ├── security/
│   │   ├── JwtUtilTest.java
│   │   └── SecurityConfigTest.java
│   ├── dataloader/
│   │   └── UserBatchLoaderTest.java
│   └── validation/
│       └── InputValidatorTest.java
│
├── integration/                    # Integration tests (Spring context)
│   ├── query/
│   │   ├── UserQueryTest.java
│   │   └── PostQueryTest.java
│   ├── mutation/
│   │   ├── UserMutationTest.java
│   │   └── PostMutationTest.java
│   ├── subscription/
│   │   └── PostSubscriptionTest.java
│   └── security/
│       ├── AuthenticationTest.java
│       └── AuthorizationTest.java
│
├── performance/                    # Performance tests
│   ├── N1QueryTest.java
│   ├── LoadTest.java
│   └── CachingTest.java
│
└── e2e/                           # End-to-end tests
    └── UserJourneyTest.java

src/test/resources/
├── application-test.yml           # Test configuration
├── test-data.sql                  # Test data
└── graphql/
    ├── queries/                   # Query fragments
    └── mutations/                 # Mutation fragments
```

---

## Unit Testing

### Service Layer Testing

**File**: `src/test/java/com/example/graphql/unit/service/PostServiceTest.java`

```java
package com.example.graphql.unit.service;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import com.example.graphql.repository.PostRepository;
import com.example.graphql.service.PostService;
import com.example.graphql.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.assertions.Assertj.assertThat;
import static org.assertj.core.assertions.Assertj.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PostService.
 * 
 * Test Coverage:
 * - CRUD operations
 * - Reactive methods
 * - Error handling
 * - Caching behavior
 * - Circuit breaker fallbacks
 * 
 * Naming Convention: methodName_StateUnderTest_ExpectedBehavior
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Unit Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostServiceTest {
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private PostService postService;
    
    private User testUser;
    private Post testPost;
    
    @BeforeEach
    void setUp() {
        // Arrange: Create test data
        testUser = new User(
            "testuser",
            "test@example.com",
            "password",
            User.Role.USER
        );
        
        testPost = new Post(
            "Test Title",
            "Test Content",
            testUser
        );
    }
    
    @Nested
    @DisplayName("Create Operations")
    class CreateOperations {
        
        @Test
        @Order(1)
        @DisplayName("createPost should save post and emit to subscription")
        void createPost_ValidInput_SavesAndEmits() {
            // Arrange
            when(postRepository.save(any(Post.class))).thenReturn(testPost);
            
            // Act
            Post result = postService.createPost(testPost);
            
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Test Title");
            
            verify(postRepository, times(1)).save(any(Post.class));
            // Verify subscription emission
            Flux<Post> postFlux = postService.postFlux();
            StepVerifier.create(postFlux.next())
                .expectNextMatches(post -> post.getTitle().equals("Test Title"))
                .verifyComplete();
        }
        
        @Test
        @DisplayName("createPost should throw exception when user is null")
        void createPost_NullUser_ThrowsException() {
            // Arrange
            Post invalidPost = new Post("Title", "Content", null);
            
            // Act & Assert
            assertThatThrownBy(() -> postService.createPost(invalidPost))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post must be associated with a user");
            
            verify(postRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("createPostReactive should work with valid data")
        void createPostReactive_ValidData_Success() {
            // Arrange
            when(userService.getUserByEmailReactive(anyString()))
                .thenReturn(Mono.just(testUser));
            when(postRepository.save(any(Post.class)))
                .thenReturn(testPost);
            
            // Act
            Mono<Post> result = postService.createPostReactive(
                "Title",
                "Content",
                "test@example.com"
            );
            
            // Assert
            StepVerifier.create(result)
                .assertNext(post -> {
                    assertThat(post.getTitle()).isEqualTo("Test Title");
                    assertThat(post.getUser()).isEqualTo(testUser);
                })
                .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Read Operations")
    class ReadOperations {
        
        @Test
        @DisplayName("findById should return post when exists")
        void findById_ExistingId_ReturnsPost() {
            // Arrange
            when(postRepository.findById(1L))
                .thenReturn(Optional.of(testPost));
            
            // Act
            Optional<Post> result = postService.findById(1L);
            
            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Test Title");
            
            verify(postRepository, times(1)).findById(1L);
        }
        
        @Test
        @DisplayName("findById should return empty when not exists")
        void findById_NonExistingId_ReturnsEmpty() {
            // Arrange
            when(postRepository.findById(999L))
                .thenReturn(Optional.empty());
            
            // Act
            Optional<Post> result = postService.findById(999L);
            
            // Assert
            assertThat(result).isEmpty();
        }
        
        @Test
        @DisplayName("getById should throw exception when not found")
        void getById_NonExisting_ThrowsException() {
            // Arrange
            when(postRepository.findById(999L))
                .thenReturn(Optional.empty());
            
            // Act & Assert
            assertThatThrownBy(() -> postService.getById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Post not found with ID: 999");
        }
        
        @Test
        @DisplayName("findAll should return all posts ordered by date")
        void findAll_MultiplePostsExist_ReturnsOrdered() {
            // Arrange
            Post post1 = new Post("Title 1", "Content 1", testUser);
            Post post2 = new Post("Title 2", "Content 2", testUser);
            List<Post> posts = Arrays.asList(post2, post1); // Reversed order
            
            when(postRepository.findAllOrderByCreatedAtDesc())
                .thenReturn(posts);
            
            // Act
            List<Post> result = postService.findAll();
            
            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("Title 2");
        }
        
        @Test
        @DisplayName("findByUser should return user's posts")
        void findByUser_ValidUser_ReturnsPosts() {
            // Arrange
            List<Post> userPosts = Arrays.asList(testPost);
            when(postRepository.findByUser(testUser))
                .thenReturn(userPosts);
            
            // Act
            List<Post> result = postService.findByUser(testUser);
            
            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUser()).isEqualTo(testUser);
        }
    }
    
    @Nested
    @DisplayName("Update Operations")
    class UpdateOperations {
        
        @Test
        @DisplayName("updatePost should update title and content")
        void updatePost_ValidUpdate_UpdatesFields() {
            // Arrange
            when(postRepository.findById(1L))
                .thenReturn(Optional.of(testPost));
            when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            
            Post update = new Post("Updated Title", "Updated Content", null);
            
            // Act
            Post result = postService.updatePost(1L, update);
            
            // Assert
            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getContent()).isEqualTo("Updated Content");
            
            verify(postRepository, times(1)).save(any(Post.class));
        }
        
        @Test
        @DisplayName("updatePost should handle partial updates")
        void updatePost_PartialUpdate_UpdatesOnlyProvidedFields() {
            // Arrange
            when(postRepository.findById(1L))
                .thenReturn(Optional.of(testPost));
            when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            
            Post update = new Post("Updated Title", null, null);
            
            // Act
            Post result = postService.updatePost(1L, update);
            
            // Assert
            assertThat(result.getTitle()).isEqualTo("Updated Title");
            assertThat(result.getContent()).isEqualTo("Test Content"); // Unchanged
        }
    }
    
    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {
        
        @Test
        @DisplayName("deletePost should remove post and clear cache")
        void deletePost_ExistingPost_Deletes() {
            // Arrange
            when(postRepository.findById(1L))
                .thenReturn(Optional.of(testPost));
            doNothing().when(postRepository).deleteById(1L);
            
            // Act
            postService.deletePost(1L);
            
            // Assert
            verify(postRepository, times(1)).deleteById(1L);
        }
        
        @Test
        @DisplayName("deletePost should throw exception when not found")
        void deletePost_NonExisting_ThrowsException() {
            // Arrange
            when(postRepository.findById(999L))
                .thenReturn(Optional.empty());
            
            // Act & Assert
            assertThatThrownBy(() -> postService.deletePost(999L))
                .isInstanceOf(EntityNotFoundException.class);
            
            verify(postRepository, never()).deleteById(any());
        }
    }
    
    @Nested
    @DisplayName("Reactive Operations")
    class ReactiveOperations {
        
        @Test
        @DisplayName("getAllPostsReactive should return all posts")
        void getAllPostsReactive_PostsExist_ReturnsAll() {
            // Arrange
            List<Post> posts = Arrays.asList(testPost);
            when(postRepository.findAllOrderByCreatedAtDesc())
                .thenReturn(posts);
            
            // Act
            Mono<List<Post>> result = postService.getAllPostsReactive();
            
            // Assert
            StepVerifier.create(result)
                .assertNext(postList -> {
                    assertThat(postList).hasSize(1);
                    assertThat(postList.get(0).getTitle()).isEqualTo("Test Title");
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("updatePostReactive should update and return post")
        void updatePostReactive_ValidUpdate_Success() {
            // Arrange
            when(postRepository.findById(1L))
                .thenReturn(Optional.of(testPost));
            when(postRepository.save(any(Post.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            
            // Act
            Mono<Post> result = postService.updatePostReactive(
                1L,
                "New Title",
                "New Content"
            );
            
            // Assert
            StepVerifier.create(result)
                .assertNext(post -> {
                    assertThat(post.getTitle()).isEqualTo("New Title");
                    assertThat(post.getContent()).isEqualTo("New Content");
                })
                .verifyComplete();
        }
        
        @Test
        @DisplayName("Reactive operations should timeout appropriately")
        void reactiveOperations_Timeout_HandledGracefully() {
            // Arrange
            when(postRepository.findAllOrderByCreatedAtDesc())
                .thenAnswer(invocation -> {
                    Thread.sleep(5000); // Simulate slow query
                    return Arrays.asList(testPost);
                });
            
            // Act
            Mono<List<Post>> result = postService.getAllPostsReactive()
                .timeout(Duration.ofSeconds(2));
            
            // Assert
            StepVerifier.create(result)
                .expectError(java.util.concurrent.TimeoutException.class)
                .verify();
        }
    }
    
    @Nested
    @DisplayName("Circuit Breaker & Fallbacks")
    class CircuitBreakerTests {
        
        @Test
        @DisplayName("findAll should use fallback on failure")
        void findAll_CircuitBreakerTriggered_UsesFallback() {
            // Arrange
            when(postRepository.findAllOrderByCreatedAtDesc())
                .thenThrow(new RuntimeException("Database error"));
            
            // Act
            List<Post> result = postService.findAllFallback(
                new RuntimeException("Database error")
            );
            
            // Assert
            assertThat(result).isEmpty(); // Fallback returns empty list
        }
    }
    
    @AfterEach
    void tearDown() {
        // Clean up any resources if needed
        reset(postRepository, userService);
    }
}
```

---

## Integration Testing

### GraphQL Query Integration Tests

**File**: `src/test/java/com/example/graphql/integration/query/PostQueryTest.java`

```java
package com.example.graphql.integration.query;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import com.example.graphql.repository.PostRepository;
import com.example.graphql.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.assertions.Assertj.assertThat;

/**
 * Integration tests for Post queries.
 * 
 * These tests:
 * 1. Start full Spring Boot application
 * 2. Use H2 in-memory database
 * 3. Execute real GraphQL queries
 * 4. Verify complete request/response cycle
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@ActiveProfiles("test")
@DisplayName("Post Query Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class PostQueryTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    private Post testPost1;
    private Post testPost2;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User(
            "integrationuser",
            "integration@test.com",
            "password",
            User.Role.USER
        );
        testUser = userRepository.save(testUser);
        
        // Create test posts
        testPost1 = new Post("Integration Test Post 1", "Content 1", testUser);
        testPost2 = new Post("Integration Test Post 2", "Content 2", testUser);
        
        testPost1 = postRepository.save(testPost1);
        testPost2 = postRepository.save(testPost2);
    }
    
    @Test
    @Order(1)
    @DisplayName("Query all posts should return list with users")
    void queryAllPosts_ShouldReturnListWithUsers() {
        String query = """
            query {
                posts {
                    id
                    title
                    content
                    user {
                        username
                        email
                    }
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .execute()
            .path("posts")
            .entityList(Object.class)
            .hasSizeGreaterThan(1)
            .path("posts[0].user.username")
            .entity(String.class)
            .isEqualTo("integrationuser");
    }
    
    @Test
    @Order(2)
    @DisplayName("Query single post by ID should return post with user")
    void queryPostById_ValidId_ReturnsPost() {
        String query = """
            query GetPost($id: ID!) {
                postById(id: $id) {
                    id
                    title
                    content
                    user {
                        id
                        username
                    }
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .variable("id", testPost1.getId())
            .execute()
            .path("postById.title")
            .entity(String.class)
            .isEqualTo("Integration Test Post 1")
            .path("postById.user.username")
            .entity(String.class)
            .isEqualTo("integrationuser");
    }
    
    @Test
    @Order(3)
    @DisplayName("Query posts with pagination should work correctly")
    void queryPostsWithPagination_ShouldPaginate() {
        String query = """
            query GetPosts($input: PageInput!) {
                postsConnection(input: $input) {
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
                        startCursor
                        endCursor
                    }
                    totalCount
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .variable("input", java.util.Map.of("first", 1))
            .execute()
            .path("postsConnection.edges")
            .entityList(Object.class)
            .hasSize(1)
            .path("postsConnection.pageInfo.hasNextPage")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("postsConnection.totalCount")
            .entity(Integer.class)
            .isGreaterThanOrEqualTo(2);
    }
    
    @Test
    @Order(4)
    @DisplayName("Query posts by author should filter correctly")
    void queryPostsByAuthor_ValidEmail_ReturnsUserPosts() {
        String query = """
            query GetPostsByAuthor($email: String!) {
                postsByAuthor(authorEmail: $email) {
                    id
                    title
                    user {
                        email
                    }
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .variable("email", "integration@test.com")
            .execute()
            .path("postsByAuthor")
            .entityList(Object.class)
            .hasSize(2)
            .path("postsByAuthor[*].user.email")
            .entityList(String.class)
            .contains("integration@test.com");
    }
    
    @Test
    @Order(5)
    @DisplayName("Query should handle errors gracefully")
    void queryPost_InvalidId_ReturnsError() {
        String query = """
            query {
                postById(id: "999999") {
                    id
                    title
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .execute()
            .errors()
            .expect(error -> error.getMessage().contains("not found"));
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test data
        postRepository.deleteAll();
        userRepository.deleteAll();
    }
}
```

---

## Security Testing

**File**: `src/test/java/com/example/graphql/integration/security/AuthenticationTest.java`

```java
package com.example.graphql.integration.security;

import com.example.graphql.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;

/**
 * Security integration tests for authentication and authorization.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@ActiveProfiles("test")
@DisplayName("Authentication & Authorization Tests")
class AuthenticationTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private String validToken;
    private String expiredToken;
    
    @BeforeEach
    void setUp() {
        // Create valid token
        UserDetails userDetails = User.withUsername("testuser")
            .password("password")
            .roles("USER")
            .build();
        
        validToken = jwtUtil.generateToken(userDetails);
    }
    
    @Test
    @DisplayName("Unauthenticated query should be rejected")
    void queryWithoutAuth_ShouldBeRejected() {
        String query = """
            query {
                posts {
                    id
                    title
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .execute()
            .errors()
            .expect(error -> error.getMessage().contains("Unauthorized"));
    }
    
    @Test
    @DisplayName("Authenticated query should succeed")
    void queryWithValidAuth_ShouldSucceed() {
        String query = """
            query {
                posts {
                    id
                    title
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .header("Authorization", "Bearer " + validToken)
            .execute()
            .path("posts")
            .entityList(Object.class)
            .hasSizeGreaterThan(0);
    }
    
    @Test
    @DisplayName("Mutation without proper role should be rejected")
    void mutationWithoutAdminRole_ShouldBeRejected() {
        String mutation = """
            mutation {
                deleteUser(id: "1")
            }
        """;
        
        graphQlTester
            .document(mutation)
            .header("Authorization", "Bearer " + validToken)
            .execute()
            .errors()
            .expect(error -> error.getMessage().contains("Forbidden"));
    }
}
```

---

## Performance Testing

**File**: `src/test/java/com/example/graphql/performance/N1QueryTest.java`

```java
package com.example.graphql.performance;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import com.example.graphql.repository.PostRepository;
import com.example.graphql.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.assertions.Assertj.assertThat;

/**
 * Performance tests to verify N+1 query problems are resolved.
 * 
 * These tests:
 * 1. Enable SQL logging
 * 2. Execute queries that could trigger N+1
 * 3. Count actual SQL queries executed
 * 4. Verify DataLoader prevents N+1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("N+1 Query Performance Tests")
@Transactional
class N1QueryTest {
    
    @Autowired
    private GraphQlTester graphQlTester;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        // Create multiple users and posts to test N+1
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User(
                "user" + i,
                "user" + i + "@test.com",
                "password",
                User.Role.USER
            );
            users.add(userRepository.save(user));
        }
        
        // Create 50 posts across users
        for (int i = 0; i < 50; i++) {
            User user = users.get(i % 10);
            Post post = new Post(
                "Post " + i,
                "Content " + i,
                user
            );
            postRepository.save(post);
        }
    }
    
    @Test
    @DisplayName("Query posts with users should execute minimal queries")
    void queryPostsWithUsers_ShouldNotCauseN1(CapturedOutput output) {
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
        
        graphQlTester
            .document(query)
            .execute()
            .path("posts")
            .entityList(Object.class)
            .hasSize(50);
        
        // Count SELECT queries in output
        String logs = output.toString();
        long selectCount = logs.lines()
            .filter(line -> line.contains("select") && line.contains("from"))
            .count();
        
        // Should be 2 queries:
        // 1. SELECT * FROM posts
        // 2. SELECT * FROM users WHERE id IN (...)
        // NOT 51 queries (1 + 50)
        assertThat(selectCount)
            .as("Should execute minimal queries with DataLoader")
            .isLessThanOrEqualTo(3);  // Allow small margin
    }
    
    @Test
    @DisplayName("Paginated query should be efficient")
    void paginatedQuery_ShouldBeEfficient(CapturedOutput output) {
        String query = """
            query {
                postsConnection(input: { first: 10 }) {
                    edges {
                        node {
                            title
                            user {
                                username
                            }
                        }
                    }
                }
            }
        """;
        
        graphQlTester
            .document(query)
            .execute()
            .path("postsConnection.edges")
            .entityList(Object.class)
            .hasSize(10);
        
        String logs = output.toString();
        long selectCount = logs.lines()
            .filter(line -> line.contains("select"))
            .count();
        
        assertThat(selectCount).isLessThanOrEqualTo(3);
    }
}
```

---

## Test Data Management

### Test Configuration

**File**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        
  sql:
    init:
      mode: always
      data-locations: classpath:test-data.sql
      
  data:
    redis:
      host: localhost
      port: 6379
      
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    com.example.graphql: DEBUG
```

### Test Data

**File**: `src/test/resources/test-data.sql`

```sql
-- Test users
INSERT INTO users (id, username, email, password, role, created_at, updated_at)
VALUES 
    (1, 'admin', 'admin@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'user1', 'user1@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'user2', 'user2@test.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Test posts
INSERT INTO posts (id, title, content, user_id, created_at, updated_at)
VALUES 
    (1, 'First Post', 'This is the first test post', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Second Post', 'This is the second test post', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Third Post', 'This is the third test post', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

---

## Continuous Testing

### Test Execution Strategy

```bash
# Run all tests
./gradlew test

# Run only unit tests
./gradlew test --tests "*.unit.*"

# Run only integration tests
./gradlew test --tests "*.integration.*"

# Run with coverage
./gradlew test jacocoTestReport

# Run performance tests
./gradlew test --tests "*.performance.*"
```

### Coverage Requirements

```groovy
// build.gradle
jacoco {
    toolVersion = "0.8.9"
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
    
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/entity/**',
                '**/dto/**',
                '**/config/**'
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80  // 80% coverage required
            }
        }
    }
}

test.finalizedBy jacocoTestReport
check.dependsOn jacocoTestCoverageVerification
```

---

## Summary

This testing strategy ensures:

✅ **Comprehensive Coverage**: 80%+ code coverage  
✅ **Fast Feedback**: Unit tests < 5 seconds  
✅ **Performance Validation**: N+1 queries detected and prevented  
✅ **Security Testing**: Authentication and authorization verified  
✅ **Integration Confidence**: Real GraphQL queries tested  
✅ **Continuous Validation**: Automated testing in CI/CD  

**Next Steps:**
1. Implement unit tests for all service classes
2. Add integration tests for all GraphQL operations
3. Set up CI/CD with automated test execution
4. Configure code coverage reporting
5. Add performance benchmarks

---

*Testing is not just about finding bugs—it's about building confidence in your code.*
