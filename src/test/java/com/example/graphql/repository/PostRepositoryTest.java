package com.example.graphql.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(excludeAutoConfiguration = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
@DisplayName("PostRepository Tests")
class PostRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password", User.Role.USER);
        entityManager.persist(testUser);

        Post post1 = new Post("Title 1", "Content 1", testUser);
        Post post2 = new Post("Title 2", "Content 2", testUser);
        entityManager.persist(post1);
        entityManager.persist(post2);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find posts by user")
    void testFindByUser() {
        List<Post> posts = postRepository.findByUser(testUser);
        assertThat(posts).hasSize(2);
        assertThat(posts.get(0).getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should find posts by user id")
    void testFindByUserId() {
        List<Post> posts = postRepository.findByUserId(testUser.getId());
        assertThat(posts).hasSize(2);
    }

    @Test
    @DisplayName("Should find all posts ordered by creation date descending")
    void testFindAllOrderByCreatedAtDesc() {
        List<Post> posts = postRepository.findAllOrderByCreatedAtDesc();
        assertThat(posts).hasSize(2);
        assertThat(posts.get(0).getCreatedAt()).isAfterOrEqualTo(posts.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("Should find posts by user ordered by creation date descending with pagination")
    void testFindByUserOrderByCreatedAtDesc() {
        Page<Post> postPage = postRepository.findByUserOrderByCreatedAtDesc(testUser, PageRequest.of(0, 1));
        assertThat(postPage.getContent()).hasSize(1);
        assertThat(postPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count posts by user")
    void testCountByUser() {
        long count = postRepository.countByUser(testUser);
        assertThat(count).isEqualTo(2);
    }
}
