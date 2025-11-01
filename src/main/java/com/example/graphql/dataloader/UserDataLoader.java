package com.example.graphql.dataloader;

import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class UserDataLoader {
    @Bean
    public DataLoader<Long, User> userDataLoader(UserRepository userRepository) {
        BatchLoader<Long, User> batchLoader = ids -> CompletableFuture.supplyAsync(
                () -> userRepository.findAllById(ids).stream().collect(Collectors.toList()));
        return DataLoader.newDataLoader(batchLoader);
    }
}
