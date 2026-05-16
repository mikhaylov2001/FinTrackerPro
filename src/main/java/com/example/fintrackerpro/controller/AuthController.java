package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.*;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.repository.UserRepository;
import com.example.fintrackerpro.service.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.example.fintrackerpro.security.GoogleIdTokenVerifierFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
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
    private final GoogleIdTokenVerifierFactory googleVerifierFactory;
    private final MetricsService metricsService;
    private final PasswordResetServiceBase passwordResetServiceBase;
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;
    public AuthController(
            UserService userService,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            AuthTokenIssuer authTokenIssuer,
            UserRepository userRepository,
            MetricsService metricsService,
            PasswordResetServiceBase passwordResetServiceBase,
            GoogleIdTokenVerifierFactory googleVerifierFactory
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.authTokenIssuer = authTokenIssuer;
        this.userRepository = userRepository;
        this.metricsService = metricsService;
        this.passwordResetServiceBase = passwordResetServiceBase;
        this.googleVerifierFactory = googleVerifierFactory;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> authConfig() {
        return ResponseEntity.ok(Map.of(
                "googleOAuthConfigured", googleVerifierFactory.isConfigured(),
                "googleAudienceSuffixes", googleVerifierFactory.audienceSuffixes()
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request, HttpServletResponse response) {
        log.info("Registration attempt for email: {}", request.getEmail());

        try {
            User user = userService.registerUser(request);

            // Успешная регистрация — зажигаем лампочку на мониторе!
            metricsService.incRegistration();

            return authTokenIssuer.issueTokens(user, response, HttpStatus.CREATED);
        } catch (Exception e) {
            // Если регистрация провалилась (например, email занят) — фиксируем бизнес-ошибку
            metricsService.incBusinessError();
            throw e; // Пробрасываем ошибку дальше, чтобы Spring вернул правильный статус
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            User user = userService.getUserEntityByEmail(request.getEmail());

            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                metricsService.incLoginFailure(); // <--- ГРАФИК "ОШИБКИ ВХОДА" ОЖИВЕТ
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid credentials"));
            }

            metricsService.incLoginSuccess(); // <--- ГРАФИК "ПРОЦЕНТ УСПЕШНЫХ ЛОГИНОВ" ПОЙДЕТ ВВЕРХ
            return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);

        } catch (Exception e) {
            metricsService.incLoginFailure(); // <--- ЛЮБАЯ ОШИБКА ПРИ ВХОДЕ ИДЕТ В СТАТИСТИКУ
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Auth failed"));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleTokenRequest request, HttpServletResponse response) {
        try {
            if (!googleVerifierFactory.isConfigured()) {
                log.error("Google sign-in rejected: GOOGLE_CLIENT_ID is not set on the server");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                        "error", "GOOGLE_NOT_CONFIGURED",
                        "message", "Google OAuth is not configured on the server (set GOOGLE_CLIENT_ID)"
                ));
            }

            if (request == null || !StringUtils.hasText(request.getIdToken())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "GOOGLE_TOKEN_REQUIRED",
                        "message", "Google token is required"
                ));
            }

            GoogleIdTokenVerifier verifier = googleVerifierFactory.verifier();
            GoogleIdToken token;
            try {
                token = verifier.verify(request.getIdToken());
            } catch (IOException | IllegalArgumentException e) {
                log.warn("Google token parse/verify error: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "INVALID_GOOGLE_TOKEN",
                        "message", "Invalid Google token"
                ));
            }

            if (token == null) {
                log.warn(
                        "Google token verification failed. Server audiences end with: {}",
                        googleVerifierFactory.audienceSuffixes()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "INVALID_GOOGLE_TOKEN",
                        "message", "Invalid Google token (client id mismatch or expired)"
                ));
            }

            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");

            if (!StringUtils.hasText(email) || !StringUtils.hasText(googleId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "GOOGLE_PROFILE_INCOMPLETE",
                        "message", "Google profile does not contain email"
                ));
            }

            Optional<User> byGoogleId = userRepository.findByGoogleId(googleId);
            if (byGoogleId.isPresent()) {
                return authTokenIssuer.issueTokens(byGoogleId.get(), response, HttpStatus.OK);
            }

            Optional<User> byEmail = userRepository.findByEmail(email);
            User user;

            if (byEmail.isPresent()) {
                user = byEmail.get();
                if (user.getGoogleId() != null && !googleId.equals(user.getGoogleId())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                            "error", "GOOGLE_ACCOUNT_CONFLICT",
                            "message", "This email is linked to another Google account"
                    ));
                }
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                    userRepository.save(user);
                }
            } else {
                user = userService.registerUserViaGoogle(email, googleId, name);
            }

            metricsService.incLoginSuccess();
            return authTokenIssuer.issueTokens(user, response, HttpStatus.OK);
        } catch (DataIntegrityViolationException e) {
            log.error("Google Auth DB constraint", e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "GOOGLE_ACCOUNT_CONFLICT",
                    "message", "Google account is linked to another user"
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Google Auth business error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "GOOGLE_REGISTRATION_FAILED",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Google Auth Error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "GOOGLE_AUTH_FAILED",
                    "message", "Google sign-in failed. Try again later"
            ));
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
                .sameSite("None")
                .path("/")
                .maxAge(0) // Удаляет куку немедленно
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetServiceBase.initiatePasswordReset(request.getEmail(), frontendUrl);
        return ResponseEntity.ok(
                new MessageResponse("Если email зарегистрирован, мы отправили письмо с инструкциями")
        );
    }

    // запрос на сохранение нового пароля
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetServiceBase.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Пароль успешно изменён"));
    }
}
