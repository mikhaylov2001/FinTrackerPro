package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.AuthRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.service.AuthTokenIssuer;
import com.example.fintrackerpro.service.RefreshTokenService;
import com.example.fintrackerpro.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthTokenIssuer authTokenIssuer;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request,
                                      HttpServletResponse response) {
        User user = userService.registerUser(request);
        return authTokenIssuer.issueTokens(user, response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request,
                                   HttpServletResponse response) {
        try {
            User user = userService.getUserEntityByEmail(request.getEmail());
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }
            return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.warn("Refresh attempt without cookies");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No cookies"));
        }

        String refresh = getValue(cookies, "refreshToken");
        String refreshId = getValue(cookies, "refreshId");

        if (refresh == null || refreshId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing tokens"));
        }

        // Валидация и ротация в БД
        var stored = refreshTokenService.validateAndGet(refreshId, refresh);
        if (stored == null) {
            log.error("Invalid or expired refresh token for ID: {}", refreshId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
        }

        User user = userService.getUserEntityById(stored.userId());
        log.info("Refreshing tokens for user: {}", user.getEmail());

        return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refreshId", required = false) String refreshId,
            HttpServletResponse response
    ) {
        // 1. Если есть ID сессии, удаляем её из базы данных
        if (refreshId != null) {
            try {
                refreshTokenService.revoke(refreshId);
                log.info("Session revoked in DB for refreshId: {}", refreshId);
            } catch (Exception e) {
                log.warn("Could not revoke session in DB: {}", e.getMessage());
            }
        }

        // 2. Стираем куки в браузере
        clearCookie(response, "refreshToken");
        clearCookie(response, "refreshId");

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private String getValue(Cookie[] cookies, String name) {
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}