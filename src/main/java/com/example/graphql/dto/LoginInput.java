package com.example.graphql.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Input DTO for login mutation.
 */
public class LoginInput {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    // Default constructor for Jackson
    public LoginInput() {}

    public LoginInput(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
