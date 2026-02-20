package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.PublicUserDto;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    // 7 дней
    private final long refreshMs = 604800000L;

    public ResponseEntity<?> issueTokens(User user, HttpServletResponse resp, HttpStatus status) {
        String access = jwtUtil.generateAccessToken(user.getId());
        String refresh = jwtUtil.generateRefreshToken(user.getId());

        String refreshId = UUID.randomUUID().toString();
        Instant refreshExp = Instant.now().plusMillis(refreshMs);

        refreshTokenService.create(refreshId, user.getId(), refresh, refreshExp);

        // После Vercel proxy (same-origin) можно поставить Lax.
        // Если вдруг будешь снова ходить cross-site напрямую на Railway — верни None.
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/") // проще и надёжнее
                .maxAge(refreshMs / 1000)
                .build();

        ResponseCookie cookieId = ResponseCookie.from("refreshId", refreshId)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(refreshMs / 1000)
                .build();

        resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, cookieId.toString());

        return ResponseEntity.status(status).body(Map.of(
                "token", access,
                "user", new PublicUserDto(
                        user.getId(),
                        user.getUserName(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName()
                )
        ));
    }
}
