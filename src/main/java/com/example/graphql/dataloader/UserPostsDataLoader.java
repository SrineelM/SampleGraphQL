package com.example.graphql.dataloader;

import com.example.graphql.entity.Post;
import com.example.graphql.repository.PostRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.dataloader.BatchLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DataLoader for batching User.posts field resolution.
 *
 * <p>Solves the N+1 query problem in GraphQL by:
 * - Collecting multiple individual post lookups
 * - Batching them into a single database query
 * - Distributing results back to individual resolvers
 *
 * <p>Example:
 * Before: Query for user 1 posts (1 query) + user 2 posts (1 query) + user 3 posts (1 query) = 3 queries
 * After: All user IDs collected, single query to fetch all posts, results distributed = 1 query
 *
 * @see org.dataloader.BatchLoader
 * @see org.dataloader.DataLoader
 */
@Component
public class UserPostsDataLoader implements BatchLoader<Long, List<Post>> {

    private static final Logger logger = LoggerFactory.getLogger(UserPostsDataLoader.class);
    private final PostRepository postRepository;

    public UserPostsDataLoader(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * Loads posts for multiple users in a single batch query.
     *
     * <p>Spring DataLoader orchestrates calling this method with accumulated user IDs.
     *
     * @param userIds the accumulated list of user IDs to fetch posts for
     * @return CompletableFuture with posts grouped by user ID, maintaining order
     */
    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<List<Post>>> load(List<Long> userIds) {
        logger.debug("Loading posts for {} users in batch", userIds.size());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Single database query for all users' posts
                List<Post> allPosts = postRepository.findByUserIdIn(userIds);
                logger.debug("Loaded {} posts for {} users", allPosts.size(), userIds.size());

                // Group posts by user ID
                Map<Long, List<Post>> postsByUser = allPosts.stream()
                        .collect(Collectors.groupingBy(post -> post.getUser().getId()));

                // Return posts in the same order as requested user IDs
                List<List<Post>> result = userIds.stream()
                        .map(userId -> postsByUser.getOrDefault(userId, Collections.emptyList()))
                        .collect(Collectors.toList());

                return (List<List<Post>>) (Object) result;

            } catch (Exception e) {
                logger.error("Error loading posts for users", e);
                // Return empty lists on error to prevent breaking the entire query
                return (List<List<Post>>) (Object)
                        userIds.stream().map(id -> Collections.emptyList()).collect(Collectors.toList());
            }
        });
    }

    /**
     * Helper method for batch loading when a Set of user IDs is available.
     * Useful for testing and alternative loading patterns.
     *
     * @param userIds the set of unique user IDs
     * @return map of user IDs to their posts
     */
    public Map<Long, List<Post>> loadSync(Set<Long> userIds) {
        try {
            List<Post> allPosts = postRepository.findByUserIdIn(new java.util.ArrayList<>(userIds));
            return allPosts.stream()
                    .collect(Collectors.groupingBy(post -> post.getUser().getId()));
        } catch (Exception e) {
            logger.error("Error in sync post loading", e);
            return Collections.emptyMap();
        }
    }
}
