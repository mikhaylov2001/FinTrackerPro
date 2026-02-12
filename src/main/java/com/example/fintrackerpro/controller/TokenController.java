package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.security.JwtUtil;
import com.example.fintrackerpro.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenController {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @CookieValue(name = "refreshId", required = false) String refreshId,
            HttpServletResponse resp
    ) {
        if (refreshToken == null || refreshId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No refresh token"));
        }

        try {
            // 1) проверяем JWT refresh (issuer/aud/sign/exp)
            Long userId = jwtUtil.extractUserId(refreshToken);
            String typ = jwtUtil.extractType(refreshToken);
            if (!"refresh".equals(typ)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid refresh token"));
            }

            // 2) проверяем, что refreshId активен и hash совпал
            refreshTokenService.requireActive(refreshId, refreshToken);

            // 3) ротация: старый refresh ревокаем, выдаём новый
            refreshTokenService.revoke(refreshId);

            String newAccess = jwtUtil.generateAccessToken(userId);
            String newRefresh = jwtUtil.generateRefreshToken(userId);

            String newRefreshId = UUID.randomUUID().toString();
            Instant newExp = Instant.now().plusMillis(604800000L);
            refreshTokenService.create(newRefreshId, userId, newRefresh, newExp);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh)
                    .httpOnly(true).secure(true).sameSite("None")
                    .path("/api/auth").maxAge(604800000L / 1000).build();

            ResponseCookie cookieId = ResponseCookie.from("refreshId", newRefreshId)
                    .httpOnly(true).secure(true).sameSite("None")
                    .path("/api/auth").maxAge(604800000L / 1000).build();

            resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            resp.addHeader(HttpHeaders.SET_COOKIE, cookieId.toString());

            return ResponseEntity.ok(Map.of("token", newAccess));

        }  catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid refresh token", "details", e.getMessage()));
    }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refreshId", required = false) String refreshId,
            HttpServletResponse resp
    ) {
        if (refreshId != null) refreshTokenService.revoke(refreshId);

        // стираем cookies
        ResponseCookie del1 = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).sameSite("None")
                .path("/api/auth").maxAge(0).build();

        ResponseCookie del2 = ResponseCookie.from("refreshId", "")
                .httpOnly(true).secure(true).sameSite("None")
                .path("/api/auth").maxAge(0).build();

        resp.addHeader(HttpHeaders.SET_COOKIE, del1.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, del2.toString());

        return ResponseEntity.noContent().build();
    }
}
