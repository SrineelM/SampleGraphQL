package com.example.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

@SpringBootTest
public class GraphQLIntegrationTest {
    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testUsersQuery() {
        graphQlTester.document("{ users { id username email } }")
                .execute()
                .path("users")
                .entityList(Object.class)
                .hasSizeGreaterThan(0);
    }
}
