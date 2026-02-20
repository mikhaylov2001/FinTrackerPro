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
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                     HttpServletResponse response) {
        // достаём куки
        String refresh = Arrays.stream(Optional.ofNullable(request.getCookies())
                        .orElse(new Cookie[0]))
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        String refreshId = Arrays.stream(Optional.ofNullable(request.getCookies())
                        .orElse(new Cookie[0]))
                .filter(c -> "refreshId".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (refresh == null || refreshId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No refresh token"));
        }

        // валидация refresh в БД
        var stored = refreshTokenService.validateAndGet(refreshId, refresh);
        if (stored == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }

        Long userId = stored.userId();
        User user = userService.getUserEntityById(userId);

        // выдаём новый access + новый refresh (ротация)
        return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);
    }

}
