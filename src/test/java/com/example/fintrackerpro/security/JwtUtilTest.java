package com.example.fintrackerpro.security;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secretKey;
    private long jwtExpirationMs;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Тестовый секретный ключ (минимум 256 бит)
        secretKey = "test-secret-key-for-jwt-token-generation-must-be-at-least-256-bits-long-for-hs256-algorithm";
        jwtExpirationMs = 3600000; // 1 час

        // Устанавливаем через рефлексию (как Spring через @Value)
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", jwtExpirationMs);
    }

    @Test
    @DisplayName("generateToken - генерация токена для username")
    void generateToken_Success() {
        // Given
        String username = "testuser";

        // When
        String token = jwtUtil.generateToken(username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT: header.payload.signature
    }

    @Test
    @DisplayName("extractUserName - извлечение username из токена")
    void extractUserName_Success() {
        // Given
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // When
        String extractedUsername = jwtUtil.extractUserName(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("extractUserId - извлечение userId из токена (username как число)")
    void extractUserId_ValidNumericUsername_ReturnsUserId() {
        // Given
        String userId = "123";
        String token = jwtUtil.generateToken(userId);

        // When
        Long extractedUserId = jwtUtil.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(123L);
    }

    @Test
    @DisplayName("extractUserId - не числовой username возвращает null")
    void extractUserId_NonNumericUsername_ReturnsNull() {
        // Given
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // When
        Long extractedUserId = jwtUtil.extractUserId(token);

        // Then
        assertThat(extractedUserId).isNull();
    }

    @Test
    @DisplayName("isTokenValid - валидный токен с UserDetails")
    void isTokenValid_ValidToken_ReturnsTrue() {
        // Given
        String username = "testuser";
        String token = jwtUtil.generateToken(username);
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        // When
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid - неправильный username")
    void isTokenValid_WrongUsername_ReturnsFalse() {
        // Given
        String token = jwtUtil.generateToken("testuser");
        UserDetails userDetails = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        // When
        boolean isValid = jwtUtil.isTokenValid(token, userDetails);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid (String) - валидный токен с username")
    void isTokenValidWithUsername_ValidToken_ReturnsTrue() {
        // Given
        String username = "testuser";
        String token = jwtUtil.generateToken(username);

        // When
        boolean isValid = jwtUtil.isTokenValid(token, username);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenExpired - свежий токен не истёк")
    void isTokenExpired_FreshToken_ReturnsFalse() {
        // Given
        String token = jwtUtil.generateToken("testuser");

        // When
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("extractExpiration - получение даты истечения")
    void extractExpiration_Success() {
        // Given
        String token = jwtUtil.generateToken("testuser");

        // When
        java.util.Date expirationDate = jwtUtil.extractExpiration(token);

        // Then
        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate.getTime()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    @DisplayName("generateRefreshToken - генерация refresh токена")
    void generateRefreshToken_Success() {
        // Given
        String username = "testuser";

        // When
        String refreshToken = jwtUtil.generateRefreshToken(username);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractAllClaims - невалидный токен выбрасывает MalformedJwtException")
    void extractAllClaims_InvalidToken_ThrowsMalformedJwtException() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractUserName(invalidToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("getTokenExpirationTime - получение времени истечения")
    void getTokenExpirationTime_ReturnsConfiguredValue() {
        // When
        long expirationTime = jwtUtil.getTokenExpirationTime();

        // Then
        assertThat(expirationTime).isEqualTo(jwtExpirationMs);
    }

    @Test
    @DisplayName("generateToken - токены содержат корректную структуру")
    void generateToken_CorrectStructure() {
        // Given
        String username = "testuser";

        // When
        String token1 = jwtUtil.generateToken(username);
        String token2 = jwtUtil.generateToken(username);

        // Then
        assertThat(token1).isNotNull();
        assertThat(token2).isNotNull();

        // Оба токена валидны
        assertThat(jwtUtil.extractUserName(token1)).isEqualTo(username);
        assertThat(jwtUtil.extractUserName(token2)).isEqualTo(username);
    }

    @Test
    @DisplayName("extractUserName - пустой токен выбрасывает исключение")
    void extractUserName_EmptyToken_ThrowsException() {
        // Given
        String emptyToken = "";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractUserName(emptyToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("generateToken с extraClaims - успешная генерация")
    void generateTokenWithExtraClaims_Success() {
        // Given
        String username = "testuser";
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ADMIN");

        // When
        String token = jwtUtil.generateToken(extraClaims, username);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtUtil.extractUserName(token)).isEqualTo(username);
    }
}
