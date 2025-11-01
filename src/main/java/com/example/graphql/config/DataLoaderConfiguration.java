package com.example.graphql.config;

import com.example.graphql.dataloader.UserDataLoader;
import com.example.graphql.repository.UserRepository;
import org.dataloader.DataLoaderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class DataLoaderConfiguration {
    @Bean
    public DataLoaderRegistry dataLoaderRegistry(UserRepository userRepository) {
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("userLoader", new UserDataLoader().userDataLoader(userRepository));
        return registry;
    }
}
