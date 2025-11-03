package com.example.graphql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "encodedPassword", User.Role.USER);
        anotherUser = new User("anotheruser", "another@example.com", "encodedPassword2", User.Role.ADMIN);
    }

    @Test
    @DisplayName("Should get all users reactively")
    void testGetAllUsersReactive() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, anotherUser));

        Mono<List<User>> result = userService.getAllUsersReactive();

        StepVerifier.create(result)
                .assertNext(users -> {
                    assertEquals(2, users.size());
                    assertEquals("testuser", users.get(0).getUsername());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get user by email reactively")
    void testGetUserByEmailReactive() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Mono<User> result = userService.getUserByEmailReactive("test@example.com");

        StepVerifier.create(result)
                .assertNext(user -> assertEquals("testuser", user.getUsername()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should create a user")
    void testCreateUser() {
        String rawPassword = "password123";
        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = userService.createUser("newuser", "new@example.com", rawPassword, User.Role.USER.name());

        assertNotNull(createdUser);
        assertEquals("newuser", createdUser.getUsername());
        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should create a user reactively")
    void testCreateUserReactive() {
        String rawPassword = "password123";
        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<User> result =
                userService.createUserReactive("newuser", "new@example.com", rawPassword, User.Role.USER.name());

        StepVerifier.create(result)
                .assertNext(user -> assertEquals("newuser", user.getUsername()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should update a user using GraphQL method")
    void testUpdateUserGraphQL() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUserGraphQL(1L, "updatedUser", "updated@example.com", "newPass", "ADMIN");

        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should update a user reactively")
    void testUpdateUserReactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mono<User> result =
                userService.updateUserReactive(1L, "updatedUser", "updated@example.com", "newPass", "ADMIN");

        StepVerifier.create(result)
                .assertNext(user -> assertEquals("updatedUser", user.getUsername()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should delete a user using GraphQL method")
    void testDeleteUserGraphQL() {
        when(userRepository.existsById(1L)).thenReturn(true);
        userService.deleteUserGraphQL(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should delete a user reactively")
    void testDeleteUserReactive() {
        when(userRepository.existsById(1L)).thenReturn(true);
        Mono<Boolean> result = userService.deleteUserReactive(1L);

        StepVerifier.create(result).expectNext(true).verifyComplete();
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void testDeleteNonExistentUser() {
        when(userRepository.existsById(999L)).thenReturn(false);
        assertThrows(UsernameNotFoundException.class, () -> userService.deleteUserGraphQL(999L));
    }

    @Test
    @DisplayName("Should find user by username for authentication")
    void testFindByUsername() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        Mono<UserDetails> result = userService.findByUsername("testuser");

        StepVerifier.create(result)
                .assertNext(userDetails -> {
                    assertEquals("testuser", userDetails.getUsername());
                    assertEquals("encodedPassword", userDetails.getPassword());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should capture user data on save")
    void testUserSaveCapture() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        String rawPassword = "rawPassword";

        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser("newuser", "new@example.com", rawPassword, "USER");

        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("newuser", capturedUser.getUsername());
        assertEquals("encodedPassword", capturedUser.getPassword());
    }

    @Test
    @DisplayName("Should capture user data on update")
    void testUserUpdateCapture() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUserGraphQL(1L, "updatedName", "updated@email.com", "newPassword", "ADMIN");

        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertEquals("updatedName", capturedUser.getUsername());
        assertEquals("newEncodedPassword", capturedUser.getPassword());
        assertEquals(User.Role.ADMIN, capturedUser.getRole());
    }
}
