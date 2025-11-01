package com.example.graphql.service;

import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "encodedPassword", User.Role.USER);
    }

    // ===== Blocking Tests =====

    @Test
    @DisplayName("Should get all users successfully")
    void testGetAllUsers() {
        // Arrange
        List<User> users = Arrays.asList(testUser, new User("user2", "user2@example.com", "pass", User.Role.ADMIN));
        org.mockito.Mockito.when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        org.assertj.core.api.Assertions.assertThat(result).hasSize(2);
        org.mockito.Mockito.verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should get user by email successfully")
    void testGetUserByEmail() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserByEmail("test@example.com");

        // Assert
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(testUser);
        org.assertj.core.api.Assertions.assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user by email not found")
    void testGetUserByEmailNotFound() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
                UsernameNotFoundException.class, () -> userService.getUserByEmail("notfound@example.com"));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void testGetUserById() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(1L);

        // Assert
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(testUser);
        org.assertj.core.api.Assertions.assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when user by ID not found")
    void testGetUserByIdNotFound() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
                UsernameNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    @DisplayName("Should search users successfully")
    void testSearchUsers() {
        // Arrange
        List<User> searchResults = Arrays.asList(testUser);
        org.springframework.data.domain.PageImpl<User> page =
                new org.springframework.data.domain.PageImpl<>(searchResults);
        org.mockito.Mockito
                .when(userRepository.searchUsers(
                        org.mockito.ArgumentMatchers.eq("test"),
                        org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        // Act
        List<User> result = userService.searchUsers("test");

        // Assert
        org.assertj.core.api.Assertions.assertThat(result).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(result.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should count users successfully")
    void testCountUsers() {
        // Arrange
        org.mockito.Mockito.when(userRepository.count()).thenReturn(5L);

        // Act
        long count = userService.countUsers();

        // Assert
        org.assertj.core.api.Assertions.assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUser() {
        // Arrange
        String encodedPassword = "encodedPassword123";
        org.mockito.Mockito.when(passwordEncoder.encode("password")).thenReturn(encodedPassword);
        org.mockito.Mockito.when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn(testUser);

        // Act
        User result = userService.createUser("testuser", "test@example.com", "password", "USER");

        // Assert
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        org.assertj.core.api.Assertions.assertThat(result.getUsername()).isEqualTo("testuser");
        org.mockito.Mockito.verify(passwordEncoder).encode("password");
        org.mockito.Mockito.verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("Should throw exception on invalid role")
    void testCreateUserInvalidRole() {
        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("testuser", "test@example.com", "password", "INVALID_ROLE"));
    }

    @Test
    @DisplayName("Should update user successfully")
    void testUpdateUserGraphQL() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        org.mockito.Mockito.when(passwordEncoder.encode("newpassword")).thenReturn("encodedNewPassword");
        org.mockito.Mockito.when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn(testUser);

        // Act
        User result = userService.updateUserGraphQL(1L, "updateduser", "updated@example.com", "newpassword", "ADMIN");

        // Assert
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        org.mockito.Mockito.verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    @DisplayName("Should delete user successfully")
    void testDeleteUserGraphQL() {
        // Arrange
        org.mockito.Mockito.when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        boolean result = userService.deleteUserGraphQL(1L);

        // Assert
        org.assertj.core.api.Assertions.assertThat(result).isTrue();
        org.mockito.Mockito.verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent user")
    void testDeleteUserGraphQLNotFound() {
        // Arrange
        org.mockito.Mockito.when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
                UsernameNotFoundException.class, () -> userService.deleteUserGraphQL(999L));
    }

    @Test
    @DisplayName("Should load user by username")
    void testLoadUserByUsername() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = userService.loadUserByUsername("testuser");

        // Assert
        org.assertj.core.api.Assertions.assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should throw exception when loading non-existent user")
    void testLoadUserByUsernameNotFound() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findByUsername("notfound")).thenReturn(Optional.empty());

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
                UsernameNotFoundException.class, () -> userService.loadUserByUsername("notfound"));
    }

    // ===== Reactive Tests =====

    @Test
    @DisplayName("Should get all users reactively")
    void testGetAllUsersReactive() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        org.mockito.Mockito.when(userRepository.findAll()).thenReturn(users);

        // Act & Assert
        userService
                .getAllUsersReactive()
                .as(StepVerifier::create)
                .expectNext(users)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get user by email reactively")
    void testGetUserByEmailReactive() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act & Assert
        userService
                .getUserByEmailReactive("test@example.com")
                .as(StepVerifier::create)
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should get user by ID reactively")
    void testGetUserByIdReactive() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        userService
                .getUserByIdReactive(1L)
                .as(StepVerifier::create)
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find user by username reactively")
    void testFindByUsernameReactive() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        userService
                .findByUsernameReactive("testuser")
                .as(StepVerifier::create)
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should create user reactively")
    void testCreateUserReactive() {
        // Arrange
        String encodedPassword = "encodedPassword123";
        org.mockito.Mockito.when(passwordEncoder.encode("password")).thenReturn(encodedPassword);
        org.mockito.Mockito.when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn(testUser);

        // Act & Assert
        userService
                .createUserReactive("testuser", "test@example.com", "password", "USER")
                .as(StepVerifier::create)
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should update user reactively")
    void testUpdateUserReactive() {
        // Arrange
        org.mockito.Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        org.mockito.Mockito.when(passwordEncoder.encode("newpassword")).thenReturn("encodedNewPassword");
        org.mockito.Mockito.when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn(testUser);

        // Act & Assert
        userService
                .updateUserReactive(1L, "updateduser", "updated@example.com", "newpassword", "ADMIN")
                .as(StepVerifier::create)
                .expectNext(testUser)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should delete user reactively")
    void testDeleteUserReactive() {
        // Arrange
        org.mockito.Mockito.when(userRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        userService
                .deleteUserReactive(1L)
                .as(StepVerifier::create)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should count users reactively")
    void testCountUsersReactive() {
        // Arrange
        org.mockito.Mockito.when(userRepository.count()).thenReturn(3L);

        // Act & Assert
        userService
                .countUsersReactive()
                .as(StepVerifier::create)
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should search users reactively")
    void testSearchUsersReactive() {
        // Arrange
        List<User> searchResults = Arrays.asList(testUser);
        org.springframework.data.domain.PageImpl<User> page =
                new org.springframework.data.domain.PageImpl<>(searchResults);
        org.mockito.Mockito
                .when(userRepository.searchUsers(
                        org.mockito.ArgumentMatchers.eq("test"),
                        org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        // Act & Assert
        userService
                .searchUsersReactive("test")
                .as(StepVerifier::create)
                .expectNext(testUser)
                .verifyComplete();
    }
}
