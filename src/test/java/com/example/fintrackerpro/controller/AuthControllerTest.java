package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.AuthRequest;
import com.example.fintrackerpro.dto.AuthResponse;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.security.JwtUtil;
import com.example.fintrackerpro.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private UserRegistrationRequest registrationRequest;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword123");

        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setUserName("newuser");
        registrationRequest.setEmail("newuser@example.com");
        registrationRequest.setPassword("password123");
        registrationRequest.setChatId(123456L);

        authRequest = new AuthRequest();
        authRequest.setUserName("testuser");
        authRequest.setPassword("password123");
    }

    @Test
    @DisplayName("POST /api/auth/register - успешная регистрация")
    void register_Success() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenReturn(testUser);
        when(jwtUtil.generateToken(anyString()))
                .thenReturn("jwt_token_123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt_token_123"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.userName").value("testuser"));

        verify(userService).registerUser(any(UserRegistrationRequest.class));
        verify(jwtUtil).generateToken(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/register - email уже существует")
    void register_EmailAlreadyExists() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already registered"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    @DisplayName("POST /api/auth/register - username уже существует")
    void register_UsernameAlreadyExists() throws Exception {
        // Given
        when(userService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new IllegalArgumentException("Username already taken"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    @Test
    @DisplayName("POST /api/auth/login - успешный вход")
    void login_Success() throws Exception {
        // Given
        when(userService.getUserByUserName("testuser"))
                .thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword123"))
                .thenReturn(true);
        when(jwtUtil.generateToken("1"))
                .thenReturn("jwt_token_123");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token_123"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.userName").value("testuser"));
    }

    @Test
    @DisplayName("POST /api/auth/login - неверный пароль")
    void login_InvalidPassword() throws Exception {
        // Given
        when(userService.getUserByUserName("testuser"))
                .thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword123"))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid password"));
    }

    @Test
    @DisplayName("POST /api/auth/login - пользователь не найден")
    void login_UserNotFound() throws Exception {
        // Given
        when(userService.getUserByUserName("unknown"))
                .thenThrow(new RuntimeException("User not found"));

        AuthRequest unknownRequest = new AuthRequest();
        unknownRequest.setUserName("unknown");
        unknownRequest.setPassword("password");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownRequest)))
                .andExpect(status().isUnauthorized());
    }
}
