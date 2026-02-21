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

    private final long refreshMs = 604800000L;

    public ResponseEntity<?> issueTokens(User user, HttpServletResponse resp, HttpStatus status) {
        // 1. Генерируем токены
        String access = jwtUtil.generateAccessToken(user.getId());
        String refresh = jwtUtil.generateRefreshToken(user.getId());

        String refreshId = UUID.randomUUID().toString();
        Instant refreshExp = Instant.now().plusMillis(refreshMs);

        // 2. Сохраняем в БД.
        // ВАЖНО: Внутри refreshTokenService.create желательно удалять СТАРЫЕ токены этого юзера,
        // чтобы не копить мусор, но это может мешать, если юзер залогинен с двух устройств.
        refreshTokenService.create(refreshId, user.getId(), refresh, refreshExp);

        // 3. Создаем куки (SameSite=None + Secure для кросс-доменных запросов)
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(refreshMs / 1000)
                .build();

        ResponseCookie cookieId = ResponseCookie.from("refreshId", refreshId)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(refreshMs / 1000)
                .build();

        // 4. Добавляем заголовки
        resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, cookieId.toString());

        // КРИТИЧЕСКИ ВАЖНО: Запрещаем кэширование этого ответа на уровне прокси и браузера
        resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        resp.setHeader(HttpHeaders.PRAGMA, "no-cache");

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