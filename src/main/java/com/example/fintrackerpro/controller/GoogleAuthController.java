package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.AuthResponse;
import com.example.fintrackerpro.dto.GoogleTokenRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserDto;
import com.example.fintrackerpro.repository.UserRepository;
import com.example.fintrackerpro.security.JwtUtil;
import com.example.fintrackerpro.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;


import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Google Authentication", description = "API для аутентификации через Google OAuth 2.0")
public class GoogleAuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${google.client-id}")
    private static String googleClientId;

    @Operation(
            summary = "Вход через Google",
            description = "Аутентифицирует пользователя через Google OAuth 2.0. " +
                    "Принимает Google ID Token, проверяет его подлинность и возвращает JWT токен приложения. " +
                    "Если пользователь новый — автоматически создаёт аккаунт."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация через Google",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Некорректный Google токен",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"error\": \"Invalid Google token\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации данных",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"error\": \"Google token is required\"}")
                    )
            )
    })
    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleTokenRequest request) {
        try {
            log.info("🔑 Google OAuth: Verifying token...");

            // Проверяем Google token
            GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");

            log.info("✅ Google OAuth verified: email={}, googleId={}, name={}", email, googleId, name);

            // Проверяем, существует ли пользователь
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;

            if (existingUser.isPresent()) {
                log.info("👤 User exists, linking Google ID");
                user = existingUser.get();
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                    userRepository.save(user);
                }
            } else {
                log.info("🆕 Creating new user via Google OAuth");
                // ✅ СОЗДАЁМ НОВОГО ПОЛЬЗОВАТЕЛЯ
                user = userService.registerUserViaGoogle(email, googleId, name);
            }

            // Генерируем JWT token
            String token = jwtUtil.generateToken(String.valueOf(user.getId()));

            log.info("✅ Google OAuth success: userId={}", user.getId());

            return ResponseEntity.ok(new AuthResponse(
                    token,
                    new UserDto(user.getId(), user.getUserName(), user.getEmail())
            ));

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid Google token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid Google token")
            );
        } catch (Exception e) {
            log.error("❌ Google OAuth failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Google authentication failed: " + e.getMessage())
            );
        }
    }


    private GoogleIdToken.Payload verifyGoogleToken(String idToken) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        )
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken token = verifier.verify(idToken);
        if (token == null) {
            throw new IllegalArgumentException("Invalid ID token");
        }

        return token.getPayload();
    }
}
