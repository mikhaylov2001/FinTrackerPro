package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.PublicUserDto;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final long refreshMs = 604800000L; // 7 дней

    public ResponseEntity<?> issueTokens(User user, HttpServletResponse resp, HttpStatus status) {
        String access = jwtUtil.generateAccessToken(user.getId());
        String refresh = jwtUtil.generateRefreshToken(user.getId());
        String refreshId = UUID.randomUUID().toString();
        Instant refreshExp = Instant.now().plusMillis(refreshMs);

        refreshTokenService.create(refreshId, user.getId(), refresh, refreshExp);

        // 7 дней в секундах
        long maxAgeSec = refreshMs / 1000;

        // ВАЖНО: SameSite=Lax + Path=/ + Secure
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAgeSec)
                .build();

        ResponseCookie cookieId = ResponseCookie.from("refreshId", refreshId)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAgeSec)
                .build();

        resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, cookieId.toString());

        // Заголовки для Safari и исправления Cross-Origin
        resp.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
        resp.setHeader("Vary", "Origin");
        resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");

        return ResponseEntity.status(status).body(Map.of(
                "token", access,
                "user", new PublicUserDto(
                        user.getId(), user.getUserName(), user.getEmail(),
                        user.getFirstName(), user.getLastName()
                )
        ));
    }
}
