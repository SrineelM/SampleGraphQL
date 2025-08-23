package com.example.graphql.dto;

import com.example.graphql.entity.User;

/**
 * Authentication payload returned after successful login/registration.
 */
public class AuthPayload {
    private final String token;
    private final String refreshToken;
    private final User user;

    public AuthPayload(String token, String refreshToken, User user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public User getUser() {
        return user;
    }
}
