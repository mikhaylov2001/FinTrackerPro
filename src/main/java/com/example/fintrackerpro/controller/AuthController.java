package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.AuthRequest;
import com.example.fintrackerpro.dto.PublicUserDto;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.security.JwtUtil;
import com.example.fintrackerpro.service.RefreshTokenService;
import com.example.fintrackerpro.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // ENV/props: 7 days
    private final long refreshMs = 604800000L;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request,
                                      HttpServletResponse response) {
        User user = userService.registerUser(request);
        return issueTokens(user, response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request,
                                   HttpServletResponse response) {
        try {
            User user =  userService.getUserEntityByUserName(request.getUserName());
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
            }
            return issueTokens(user, response, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    private ResponseEntity<?> issueTokens(User user, HttpServletResponse resp, HttpStatus status) {
        String access = jwtUtil.generateAccessToken(user.getId());
        String refresh = jwtUtil.generateRefreshToken(user.getId());

        // refresh-id храним отдельно (ротация/ревокация)
        String refreshId = UUID.randomUUID().toString();
        Instant refreshExp = Instant.now().plusMillis(refreshMs);
        // сохраняем refreshId+hash(refresh) в БД
        refreshTokenService.create(refreshId, user.getId(), refresh, refreshExp);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth")        // refresh/logout живут тут
                .maxAge(refreshMs / 1000)
                .build();

        // Можно ещё cookie для refreshId (не HttpOnly) или хранить refreshId внутри refresh как claim "jti".
        // Для простоты — сделаем отдельным HttpOnly cookie:
        ResponseCookie cookieId = ResponseCookie.from("refreshId", refreshId)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth")
                .maxAge(refreshMs / 1000)
                .build();

        resp.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, cookieId.toString());

        return ResponseEntity.status(status).body(Map.of(
                "token", access,
                "user", new PublicUserDto(user.getId(), user.getUserName(), user.getEmail())
        ));
    }
}
