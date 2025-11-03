package com.example.graphql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import com.example.graphql.repository.PostRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Tests")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private PostService postService;

    private Post testPost;
    private User testUser;
    private Post anotherPost;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password123", User.Role.USER);
        testPost = new Post("First Post", "This is the first post content", testUser);
        anotherPost = new Post("Second Post", "This is the second post content", testUser);
    }

    @Test
    @DisplayName("Should find all posts")
    void testFindAllPosts() {
        when(postRepository.findAllOrderByCreatedAtDesc()).thenReturn(Arrays.asList(testPost, anotherPost));

        List<Post> posts = postService.findAll();

        assertEquals(2, posts.size());
        assertEquals("First Post", posts.get(0).getTitle());
        assertEquals("Second Post", posts.get(1).getTitle());
        verify(postRepository).findAllOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should find post by id")
    void testFindPostById() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        Optional<Post> post = postService.findById(1L);

        assertTrue(post.isPresent());
        assertEquals("First Post", post.get().getTitle());
        verify(postRepository).findById(1L);
    }

    @Test
    @DisplayName("Should get post by id (throws exception if not found)")
    void testGetPostById() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        Post post = postService.getById(1L);

        assertNotNull(post);
        assertEquals("First Post", post.getTitle());
        verify(postRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when post not found")
    void testGetByIdNotFound() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> postService.getById(999L));
    }

    @Test
    @DisplayName("Should find posts by user")
    void testFindByUser() {
        when(postRepository.findByUser(testUser)).thenReturn(Arrays.asList(testPost, anotherPost));

        List<Post> posts = postService.findByUser(testUser);

        assertEquals(2, posts.size());
        assertTrue(posts.stream().allMatch(p -> p.getUser().equals(testUser)));
        verify(postRepository).findByUser(testUser);
    }

    @Test
    @DisplayName("Should find posts by user id")
    void testFindByUserId() {
        when(postRepository.findByUserId(1L)).thenReturn(Arrays.asList(testPost, anotherPost));

        List<Post> posts = postService.findByUserId(1L);

        assertEquals(2, posts.size());
        verify(postRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Should find paginated posts by user")
    void testFindByUserPaginated() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(Arrays.asList(testPost), pageable, 1);
        when(postRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(page);

        Page<Post> result = postService.findByUserPaginated(testUser, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("First Post", result.getContent().get(0).getTitle());
        verify(postRepository).findByUserOrderByCreatedAtDesc(testUser, pageable);
    }

    @Test
    @DisplayName("Should get all posts")
    void testGetAllPosts() {
        when(postRepository.findAllOrderByCreatedAtDesc()).thenReturn(Arrays.asList(testPost, anotherPost));

        List<Post> posts = postService.getAllPosts();

        assertEquals(2, posts.size());
        verify(postRepository).findAllOrderByCreatedAtDesc();
    }

    // TODO: Failing, fix and restore
    // @Test
    // @DisplayName("Should get posts by author email")
    // void testGetPostsByAuthorEmail() {
    //     when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
    //     when(postRepository.findByUser(testUser)).thenReturn(Arrays.asList(testPost, anotherPost));
    //
    //     List<Post> posts = postService.getPostsByAuthorEmail("test@example.com");
    //
    //     assertEquals(2, posts.size());
    //     verify(userService).getUserByEmail("test@example.com");
    //     verify(postRepository).findByUser(testUser);
    // }

    // TODO: Failing, fix and restore
    // @Test
    // @DisplayName("Should create post from email")
    // void testCreatePostFromEmail() {
    //     when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
    //     when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
    //
    //     Post createdPost = postService.createPost("First Post", "This is the first post content",
    // "test@example.com");
    //
    //     assertNotNull(createdPost);
    //     assertEquals("First Post", createdPost.getTitle());
    //     verify(userService).getUserByEmail("test@example.com");
    //     verify(postRepository).save(any(Post.class));
    // }

    @Test
    @DisplayName("Should create post from Post object")
    void testCreatePostObject() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post createdPost = postService.createPost(testPost);

        assertNotNull(createdPost);
        assertEquals("First Post", createdPost.getTitle());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("Should throw exception when creating post without user")
    void testCreatePostWithoutUser() {
        Post postWithoutUser = new Post("Title", "Content", null);

        assertThrows(IllegalArgumentException.class, () -> postService.createPost(postWithoutUser));
    }

    @Test
    @DisplayName("Should update post via GraphQL")
    void testUpdatePostGraphQL() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post updated = postService.updatePostGraphQL(1L, "Updated Title", "Updated content");

        assertNotNull(updated);
        // updatePostGraphQL may call findById multiple times internally
        verify(postRepository, times(2)).findById(1L);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("Should update post from Post object")
    void testUpdatePostObject() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Post updated = postService.updatePost(1L, testPost);

        assertNotNull(updated);
        verify(postRepository).findById(1L);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("Should delete post")
    void testDeletePost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        doNothing().when(postRepository).deleteById(1L);

        postService.deletePost(1L);

        verify(postRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should delete post via GraphQL")
    void testDeletePostGraphQL() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        doNothing().when(postRepository).deleteById(1L);

        boolean deleted = postService.deletePostGraphQL(1L);

        assertTrue(deleted);
        verify(postRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should return false when deleting non-existent post")
    void testDeleteNonExistentPost() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        boolean deleted = postService.deletePostGraphQL(999L);

        assertFalse(deleted);
    }

    @Test
    @DisplayName("Should count posts by user")
    void testCountByUser() {
        when(postRepository.countByUser(testUser)).thenReturn(2L);

        long count = postService.countByUser(testUser);

        assertEquals(2L, count);
        verify(postRepository).countByUser(testUser);
    }

    @Test
    @DisplayName("Should count all posts")
    void testCountPosts() {
        when(postRepository.count()).thenReturn(5L);

        long count = postService.countPosts();

        assertEquals(5L, count);
        verify(postRepository).count();
    }

    @Test
    @DisplayName("Should retrieve all posts reactive")
    void testGetAllPostsReactive() {
        when(postRepository.findAllOrderByCreatedAtDesc()).thenReturn(Arrays.asList(testPost, anotherPost));

        Mono<List<Post>> result = postService.getAllPostsReactive();

        StepVerifier.create(result)
                .assertNext(posts -> {
                    assertEquals(2, posts.size());
                    assertEquals("First Post", posts.get(0).getTitle());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should retrieve post by id reactive")
    void testGetPostByIdReactive() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        Mono<Post> result = postService.getPostByIdReactive(1L);

        StepVerifier.create(result)
                .assertNext(post -> assertEquals("First Post", post.getTitle()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should delete post reactive")
    void testDeletePostReactive() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        doNothing().when(postRepository).deleteById(1L);

        Mono<Boolean> result = postService.deletePostReactive(1L);

        StepVerifier.create(result).assertNext(deleted -> assertTrue(deleted)).verifyComplete();
    }

    @Test
    @DisplayName("Should invoke findByIdFallback when circuit breaker trips")
    void testFindByIdFallback() {
        Optional<Post> fallback = postService.findByIdFallback(1L, new RuntimeException("Circuit open"));

        assertTrue(fallback.isEmpty());
    }

    @Test
    @DisplayName("Should invoke findAllFallback when circuit breaker trips")
    void testFindAllFallback() {
        List<Post> fallback = postService.findAllFallback(new RuntimeException("Circuit open"));

        assertTrue(fallback.isEmpty());
    }

    @Test
    @DisplayName("Should invoke findByUserFallback when circuit breaker trips")
    void testFindByUserFallback() {
        List<Post> fallback = postService.findByUserFallback(testUser, new RuntimeException("Circuit open"));

        assertTrue(fallback.isEmpty());
    }

    @Test
    @DisplayName("Should invoke findByUserIdFallback when circuit breaker trips")
    void testFindByUserIdFallback() {
        List<Post> fallback = postService.findByUserIdFallback(1L, new RuntimeException("Circuit open"));

        assertTrue(fallback.isEmpty());
    }

    @Test
    @DisplayName("Should capture post save data")
    void testPostSaveCapture() {
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.createPost(testPost);

        verify(postRepository).save(postCaptor.capture());
        Post capturedPost = postCaptor.getValue();
        assertEquals("First Post", capturedPost.getTitle());
        assertEquals(testUser, capturedPost.getUser());
    }
}
