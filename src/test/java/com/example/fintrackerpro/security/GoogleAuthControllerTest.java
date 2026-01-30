package com.example.fintrackerpro.security;

import com.example.fintrackerpro.dto.GoogleTokenRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.repository.UserRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GoogleAuthController Integration Tests")
class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    private GoogleTokenRequest googleTokenRequest;

    @BeforeEach
    void setUp() {
        googleTokenRequest = new GoogleTokenRequest();
        googleTokenRequest.setIdToken("valid.google.id.token");
    }

    @Test
    @DisplayName("POST /api/auth/google - null токен возвращает ошибку")
    void googleAuth_NullToken_ReturnsBadRequest() throws Exception {
        // Given
        GoogleTokenRequest nullRequest = new GoogleTokenRequest();
        nullRequest.setIdToken(null);

        // When & Then
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/google - пустой токен возвращает ошибку")
    void googleAuth_EmptyToken_ReturnsBadRequest() throws Exception {
        // Given
        GoogleTokenRequest emptyRequest = new GoogleTokenRequest();
        emptyRequest.setIdToken("");

        // When & Then
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/google - невалидный Google токен возвращает ошибку")
    void googleAuth_InvalidTokenFormat_ReturnsBadRequest() throws Exception {
        // Given
        GoogleTokenRequest invalidRequest = new GoogleTokenRequest();
        invalidRequest.setIdToken("invalid.token.format");

        // When & Then
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Google token"));

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/google - контроллер обрабатывает исключения")
    void googleAuth_ExceptionHandling_ReturnsBadRequest() throws Exception {
        // Given
        GoogleTokenRequest request = new GoogleTokenRequest();
        request.setIdToken("malformed.token");

        // When & Then
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/auth/google - эндпоинт существует и обрабатывает POST запросы")
    void googleAuth_EndpointExists_AcceptsPostRequests() throws Exception {
        // Given
        GoogleTokenRequest request = new GoogleTokenRequest();
        request.setIdToken("any.token.here");

        // When & Then - проверяем что эндпоинт существует и отвечает
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Невалидный токен => 400
    }

    @Test
    @DisplayName("POST /api/auth/google - возвращает JSON в ответе")
    void googleAuth_ReturnsJsonResponse() throws Exception {
        // Given
        GoogleTokenRequest request = new GoogleTokenRequest();
        request.setIdToken("test.token");

        // When & Then
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /api/auth/google - правильный Content-Type в ответе")
    void googleAuth_ResponseContentType() throws Exception {
        // Given
        GoogleTokenRequest request = new GoogleTokenRequest();
        request.setIdToken("invalid");

        // When & Then
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"));
    }
}
