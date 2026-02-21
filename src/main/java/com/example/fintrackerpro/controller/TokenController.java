package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refreshId", required = false) String refreshId,
            HttpServletResponse resp
    ) {
        if (refreshId != null) refreshTokenService.revoke(refreshId);

        ResponseCookie del1 = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")     // ВАЖНО: тот же path что и при установке
                .maxAge(0)
                .build();

        ResponseCookie del2 = ResponseCookie.from("refreshId", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")     // ВАЖНО
                .maxAge(0)
                .build();

        resp.addHeader(HttpHeaders.SET_COOKIE, del1.toString());
        resp.addHeader(HttpHeaders.SET_COOKIE, del2.toString());

        return ResponseEntity.noContent().build();
    }
}
