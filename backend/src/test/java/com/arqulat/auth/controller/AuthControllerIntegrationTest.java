package com.arqulat.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.arqulat.auth.model.User;
import com.arqulat.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arqulat.auth.dto.LoginRequest;
import com.arqulat.auth.dto.RegisterRequest;

import jakarta.servlet.http.Cookie;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.arqulat.auth.service.JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Clear the test database before each test
        userRepository.deleteAll();
    }

    @Test
    @org.junit.jupiter.api.Disabled("Disabled temporarily until OTP is implemented")
    void register_shouldReturn201_WhenDataIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("StrongPass123!");
        request.setName("New User");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.name").value("New User"));
    }

    @Test
    @org.junit.jupiter.api.Disabled("Disabled temporarily until OTP is implemented")
    void register_shouldReturn400_WhenPasswordIsWeak() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("weak");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    @org.junit.jupiter.api.Disabled("Disabled temporarily until OTP is implemented")
    void register_shouldReturn409_WhenEmailAlreadyExists() throws Exception {
        // Pre-populate DB
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setPasswordHash(passwordEncoder.encode("SomePassword123!"));
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("StrongPass123!");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    @org.junit.jupiter.api.Disabled("Disabled temporarily until OTP is implemented")
    void login_shouldReturn200AndSetCookie_WhenCredentialsAreValid() throws Exception {
        // Pre-populate DB
        User user = new User();
        user.setEmail("loginuser@example.com");
        user.setPasswordHash(passwordEncoder.encode("StrongPass123!"));
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("loginuser@example.com");
        request.setPassword("StrongPass123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("arqulat_session"))
                .andExpect(jsonPath("$.email").value("loginuser@example.com"));
    }

    @Test
    @org.junit.jupiter.api.Disabled("Disabled temporarily until OTP is implemented")
    void login_shouldReturn401_WhenPasswordIsIncorrect() throws Exception {
        // Pre-populate DB
        User user = new User();
        user.setEmail("loginuser@example.com");
        user.setPasswordHash(passwordEncoder.encode("StrongPass123!"));
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("loginuser@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_shouldRedirectToGoogle_WhenNoCookieProvided() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/oauth2/authorization/google"));
    }

    @Test
    void getCurrentUser_shouldReturnUser_WhenValidCookieProvided() throws Exception {
        // 1. Create User
        User user = new User();
        user.setEmail("me@example.com");
        user.setPasswordHash(passwordEncoder.encode("StrongPass123!"));
        userRepository.save(user);

        // 2. Generate token directly
        com.arqulat.auth.security.AppUserDetails userDetails = new com.arqulat.auth.security.AppUserDetails(user);
        String token = jwtService.generateToken(userDetails);
        Cookie jwtCookie = new Cookie("arqulat_session", token);

        // 3. Access protected endpoint with cookie
        mockMvc.perform(get("/api/v1/user/me")
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"));
    }
}
