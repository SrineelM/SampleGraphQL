package com.example.graphql.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.graphql.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "testsecretkeytestsecretkeytestsecretkeytestsecretkey");

        userDetails = new User("testuser", "test@example.com", "password", User.Role.USER);
    }

    private String generateExpiredToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2)) // 2 hours ago
                .expiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1 hour ago
                .signWith((javax.crypto.SecretKey)
                        ReflectionTestUtils.invokeMethod(jwtUtil, "getKey")) // Use helper to get key
                .compact();
    }

    @Test
    @DisplayName("Should generate a valid token")
    void testGenerateToken() {
        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should extract username from token")
    void testExtractUsername() {
        String token = jwtUtil.generateToken(userDetails);
        String username = jwtUtil.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void testExtractExpiration() {
        String token = jwtUtil.generateToken(userDetails);
        Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("Should validate a valid token")
    void testValidateToken() {
        String token = jwtUtil.generateToken(userDetails);
        assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    @DisplayName("Should not validate an expired token")
    void testValidateToken_Expired() {
        String expiredToken = generateExpiredToken(userDetails);
        // When parsing an expired token, extractUsername throws ExpiredJwtException
        // So validateToken will throw an exception rather than return false
        try {
            boolean result = jwtUtil.validateToken(expiredToken, userDetails);
            // If it doesn't throw, it should return false
            assertFalse(result);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // This is the expected behavior - expired tokens throw an exception
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Should not validate a token with incorrect username")
    void testValidateToken_IncorrectUsername() {
        String token = jwtUtil.generateToken(userDetails);
        UserDetails otherUserDetails = new User("otheruser", "other@example.com", "password", User.Role.USER);
        assertFalse(jwtUtil.validateToken(token, otherUserDetails));
    }
}
