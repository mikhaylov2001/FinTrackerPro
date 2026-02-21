package com.example.fintrackerpro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class OAuth2Controller {

    @GetMapping("/user")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of(
                "id", principal.getAttribute("sub"),
                "email", principal.getAttribute("email"),
                "name", principal.getAttribute("name"),
                "attributes", principal.getAttributes()
        ));
    }

    @GetMapping("/login/success")
    public ResponseEntity<?> loginSuccess(@AuthenticationPrincipal OAuth2User principal) {
        // Spring автоматически обработает OAuth2, мы просто возвращаем данные
        return ResponseEntity.ok(Map.of(
                "user", principal.getAttributes()
        ));
    }
}

