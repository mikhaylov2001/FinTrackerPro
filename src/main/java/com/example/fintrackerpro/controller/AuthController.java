package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.AuthRequest;
import com.example.fintrackerpro.dto.PublicUserDto;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.security.JwtUtil;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthTokenIssuer authTokenIssuer;

    // ENV/props: 7 days
    private final long refreshMs = 604800000L;

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
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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

        // Валидация. Внутри validateAndGet убедись, что старый токен УДАЛЯЕТСЯ (ротация)
        var stored = refreshTokenService.validateAndGet(refreshId, refresh);
        if (stored == null) {
            log.error("Invalid or expired refresh token for ID: {}", refreshId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
        }

        User user = userService.getUserEntityById(stored.userId());
        log.info("Refreshing tokens for user: {}", user.getEmail());

        // Выдаем новые токены
        return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);
    }

    private String getValue(Cookie[] cookies, String name) {
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Очищаем куки на стороне клиента при выходе
        clearCookie(response, "refreshToken");
        clearCookie(response, "refreshId");
        return ResponseEntity.ok(Map.of("message", "Logged out"));
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