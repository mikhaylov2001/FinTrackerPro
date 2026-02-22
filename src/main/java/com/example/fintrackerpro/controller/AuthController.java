package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.AuthRequest;
import com.example.fintrackerpro.dto.GoogleTokenRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.repository.UserRepository;
import com.example.fintrackerpro.service.AuthTokenIssuer;
import com.example.fintrackerpro.service.RefreshTokenService;
import com.example.fintrackerpro.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
// Явное указание CORS для контроллера — подстраховка к SecurityConfig
@CrossOrigin(origins = {"https://fintrackerpro.vercel.app"}, allowCredentials = "true")
@Slf4j
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthTokenIssuer authTokenIssuer;
    private final UserRepository userRepository;
    private final GoogleIdTokenVerifier verifier;

    public AuthController(
            UserService userService,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            AuthTokenIssuer authTokenIssuer,
            UserRepository userRepository,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.authTokenIssuer = authTokenIssuer;
        this.userRepository = userRepository;

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request, HttpServletResponse response) {
        log.info("Registration attempt for email: {}", request.getEmail());
        User user = userService.registerUser(request);
        return authTokenIssuer.issueTokens(user, response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        log.info("Login attempt for email: {}", request.getEmail());
        User user = userService.getUserEntityByEmail(request.getEmail());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
        return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleTokenRequest request, HttpServletResponse response) {
        try {
            if (request == null || request.getIdToken() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Google token is required"));
            }

            GoogleIdToken token = verifier.verify(request.getIdToken());
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid Google token"));
            }

            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");

            Optional<User> byEmail = userRepository.findByEmail(email);
            User user;

            if (byEmail.isPresent()) {
                user = byEmail.get();
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                    userRepository.save(user);
                }
            } else {
                user = userService.registerUserViaGoogle(email, googleId, name);
            }

            return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Google Auth Error", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Auth failed"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        String refresh = getValue(cookies, "refreshToken");
        String refreshId = getValue(cookies, "refreshId");

        if (refresh == null || refreshId == null) {
            log.warn("Refresh attempt without cookies");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No refresh token"));
        }

        var stored = refreshTokenService.validateAndGet(refreshId, refresh);
        if (stored == null) {
            log.warn("Invalid refresh session for ID: {}", refreshId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid session"));
        }

        User user = userService.getUserEntityById(stored.userId());
        return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refreshId", required = false) String refreshId,
            HttpServletResponse response
    ) {
        log.info("Logout requested for refreshId: {}", refreshId);
        if (refreshId != null) {
            refreshTokenService.revoke(refreshId);
        }

        // Очищаем обе куки
        clearCookie(response, "refreshToken");
        clearCookie(response, "refreshId");

        return ResponseEntity.noContent().build();
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
        // Важно: параметры (path, secure, sameSite) должны совпадать с теми, что были при создании
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0) // Удаляет куку немедленно
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}