package com.arqulat.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.arqulat.auth.dto.AuthResponse;
import com.arqulat.auth.dto.LoginRequest;
import com.arqulat.auth.dto.RegisterRequest;
import com.arqulat.auth.exception.ResourceNotFoundException;
import com.arqulat.auth.exception.UserAlreadyExistsException;
import com.arqulat.auth.model.User;
import com.arqulat.auth.repository.UserRepository;
import com.arqulat.auth.security.AppUserDetails;

import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "cookieDomain", "localhost");
        ReflectionTestUtils.setField(authService, "cookieMaxAge", 3600L);
    }

    @Test
    void register_shouldThrowExceptionIfEmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldSaveUserAndReturnResponse() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("Test User");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("test@example.com");
        savedUser.setName("Test User");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login_shouldReturnAuthResponseAndSetCookie_OnSuccess() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        AppUserDetails userDetails = new AppUserDetails(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        
        when(jwtService.generateToken(userDetails)).thenReturn("jwt_token_here");

        // Act
        AuthResponse result = authService.login(request, response);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(response, times(1)).addHeader(eq("Set-Cookie"), anyString());
    }

    @Test
    void login_shouldThrowException_OnBadCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(request, response));
        verify(response, never()).addHeader(anyString(), anyString());
    }

    @Test
    void getCurrentUser_shouldReturnUserIfFound() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act
        AuthResponse response = authService.getCurrentUser("test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void getCurrentUser_shouldThrowExceptionIfNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUser("unknown@example.com"));
    }
}
