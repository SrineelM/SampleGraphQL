package com.example.graphql.controller;

import com.example.graphql.dto.AuthPayload;
import com.example.graphql.dto.LoginInput;
import com.example.graphql.dto.UserInput;
import com.example.graphql.entity.User;
import com.example.graphql.security.JwtUtil;
import com.example.graphql.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @MutationMapping
    public Mono<AuthPayload> login(@Argument @jakarta.validation.Valid LoginInput input) {
        return userService.findByUsernameReactive(input.getUsername())
                .cast(User.class)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid username or password")))
                .filter(user -> passwordEncoder.matches(input.getPassword(), user.getPassword()))
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid username or password")))
                .map(user -> {
                    UserDetails userDetails = org.springframework.security.core.userdetails.User
                            .withUsername(user.getUsername())
                            .password(user.getPassword())
                            .roles(user.getRole().name())
                            .build();
                    
                    String token = jwtUtil.generateToken(userDetails);
                    return new AuthPayload(token, generateRefreshToken(user), user);
                });
    }

    @MutationMapping
    public Mono<AuthPayload> register(@Argument @jakarta.validation.Valid UserInput input) {
        return userService.createUserReactive(
                input.getUsername(),
                input.getEmail(),
                input.getPassword(),
                input.getRole() != null ? input.getRole().name() : "USER"
        ).map(user -> {
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .build();
            
            String token = jwtUtil.generateToken(userDetails);
            return new AuthPayload(token, generateRefreshToken(user), user);
        });
    }

    private String generateRefreshToken(User user) {
        // In a real application, this should be a proper refresh token
        // stored in a secure way (database, Redis, etc.)
        return "refresh_" + user.getId() + "_" + System.currentTimeMillis();
    }
}
