package com.example.graphql.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, new BCryptPasswordEncoder(12));
    }

    @Test
    void testCreateUser() {
        User user = new User("test", "test@example.com", "pwd", User.Role.USER);
        when(userRepository.save(any())).thenReturn(user);
        User result = userService.createUser("test", "test@example.com", "pwd", "USER");
        assertNotNull(result);
        assertEquals("test", result.getUsername());
    }

    @Test
    void testGetUserById() {
        User user = new User("john", "john@example.com", "pwd", User.Role.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        User result = userService.getUserById(1L);
        assertEquals("john", result.getUsername());
    }

    @Test
    void testGetAllUsers() {
        List<User> users = List.of(
                new User("user1", "user1@example.com", "pwd", User.Role.USER),
                new User("user2", "user2@example.com", "pwd", User.Role.ADMIN));
        when(userRepository.findAll()).thenReturn(users);
        List<User> result = userService.getAllUsers();
        assertEquals(2, result.size());
    }

    @Test
    void testDeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);
        boolean result = userService.deleteUserGraphQL(1L);
        assertTrue(result);
    }
}
