package com.example.graphql.security;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for JwtUtil.
 *
 * Tests JWT token generation, validation, claim extraction, and expiration handling.
 */
public class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;
    private static final String TEST_SECRET = "your-32-byte-secret-key-should-be-long-enough!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Initialize the secretKey field using ReflectionTestUtils
        ReflectionTestUtils.setField(jwtUtil, "secretKey", TEST_SECRET);

        userDetails = User.withUsername("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void testGenerateToken() {
        // Act
        String token = jwtUtil.generateToken(userDetails);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUsername() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        String extractedUsername = jwtUtil.extractUsername(token);

        // Assert
        assertEquals("testuser", extractedUsername);
    }

    @Test
    void testExtractClaim() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
        Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);

        // Assert
        assertNotNull(issuedAt);
        assertNotNull(expiration);
        assertTrue(expiration.after(issuedAt));
    }

    @Test
    void testValidateToken() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateTokenWithWrongUsername() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);
        UserDetails differentUser = User.withUsername("differentuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // Act
        boolean isValid = jwtUtil.validateToken(token, differentUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testTokenExpiration() throws InterruptedException {
        // Create a custom JwtUtil with very short expiration for testing
        JwtUtil shortExpirationJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secretKey", TEST_SECRET);

        // Override generateToken to use short expiration
        long shortExpirationTime = System.currentTimeMillis() + 100; // 100ms
        String token = Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(shortExpirationTime))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes()))
                .compact();

        // Wait to ensure token expires
        Thread.sleep(200);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            shortExpirationJwtUtil.validateToken(token, userDetails);
        });
    }
}
