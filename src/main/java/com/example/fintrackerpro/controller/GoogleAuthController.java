package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.AuthResponse;
import com.example.fintrackerpro.dto.GoogleTokenRequest;
import com.example.fintrackerpro.dto.PublicUserDto;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserDto;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
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
@Slf4j
@Tag(name = "Google Authentication", description = "API для аутентификации через Google OAuth 2.0")
public class GoogleAuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthController(
            UserService userService,
            JwtUtil jwtUtil,
            UserRepository userRepository,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId
    ) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody GoogleTokenRequest request) {
        try {
            if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Google token is required"));
            }

            GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());

            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified != null && !emailVerified) {
                return ResponseEntity.status(401).body(Map.of("error", "Email is not verified"));
            }

            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");

            if (email == null || googleId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Google token"));
            }

            // Рекомендация: сначала искать по googleId (если вы храните его уникально)
            Optional<User> byEmail = userRepository.findByEmail(email);

            User user;
            if (byEmail.isPresent()) {
                user = byEmail.get();

                // Если уже привязан другой Google ID — лучше не перетирать
                if (user.getGoogleId() != null && !user.getGoogleId().equals(googleId)) {
                    return ResponseEntity.status(409).body(Map.of("error", "Account already linked to another Google ID"));
                }

                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                    userRepository.save(user);
                }
            } else {
                user = userService.registerUserViaGoogle(email, googleId, name);
            }

            String token = jwtUtil.generateAccessToken((user.getId()));

            return ResponseEntity.ok(new AuthResponse(
                    token,
                    new PublicUserDto(user.getId(), user.getUserName(), user.getEmail(), user.getFirstName(), user.getLastName())
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid Google token"));
        } catch (Exception e) {
            log.error("Google OAuth failed", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Google authentication failed"));
        }
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idToken) throws Exception {
        GoogleIdToken token = verifier.verify(idToken);
        if (token == null) {
            throw new IllegalArgumentException("Invalid ID token");
        }
        return token.getPayload();
    }
}
