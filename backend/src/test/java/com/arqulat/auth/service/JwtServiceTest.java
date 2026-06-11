package com.arqulat.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private final String SECRET_KEY = "dGVzdFNlY3JldEtleVRoYXRJc0xvbmdFbm91Z2hGb3JIZW0="; // "testSecretKeyThatIsLongEnoughForHem"
    private final long EXPIRATION = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        // Arrange
        when(userDetails.getUsername()).thenReturn("test@example.com");

        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertNotNull(token);
        String username = jwtService.extractUserName(token);
        assertEquals("test@example.com", username);
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        // Arrange
        when(userDetails.getUsername()).thenReturn("test@example.com");
        String token = jwtService.generateToken(userDetails);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_shouldReturnFalseForDifferentUser() {
        // Arrange
        when(userDetails.getUsername()).thenReturn("test@example.com");
        String token = jwtService.generateToken(userDetails);
        
        UserDetails anotherUser = mock(UserDetails.class);
        when(anotherUser.getUsername()).thenReturn("other@example.com");

        // Act
        boolean isValid = jwtService.isTokenValid(token, anotherUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void extractUserName_shouldThrowExceptionForExpiredToken() {
        // Arrange
        when(userDetails.getUsername()).thenReturn("test@example.com");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L); // Force token to expire immediately
        String token = jwtService.generateToken(userDetails);

        // Act & Assert
        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUserName(token));
    }
}
