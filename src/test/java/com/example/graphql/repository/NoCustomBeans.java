package com.example.graphql.repository;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class NoCustomBeans {
    @Bean
    @Primary
    public com.example.graphql.dataloader.UserDataLoader userDataLoader() {
        return null;
    }

    @Bean
    @Primary
    public org.dataloader.DataLoaderRegistry dataLoaderRegistry() {
        return null;
    }
}
