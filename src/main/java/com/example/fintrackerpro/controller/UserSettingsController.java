// UserSettingsController.java
package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.entity.user.UserSettingsDto;
import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.MetricsService;
import com.example.fintrackerpro.service.UserSettingsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings")
@SecurityRequirement(name = "bearerAuth")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;
    private final MetricsService metricsService;

    @GetMapping("/me")
    public ResponseEntity<UserSettingsDto> getMySettings(Authentication auth) {
        Long userId = CurrentUser.id(auth);
        return ResponseEntity.ok(userSettingsService.getSettings(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserSettingsDto> updateMySettings(
            Authentication auth,
            @RequestBody UserSettingsDto dto
    ) {
        Long userId = CurrentUser.id(auth);
        return ResponseEntity.ok(userSettingsService.updateSettings(userId, dto));
    }
}
