package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.ChangePasswordRequest;
import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account", description = "Настройки аккаунта и безопасность")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final UserService userService;

    @Operation(summary = "Сменить пароль текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль успешно изменён"),
            @ApiResponse(responseCode = "400", description = "Неверные данные или слабый пароль"),
            @ApiResponse(responseCode = "401", description = "Не авторизован или текущий пароль неверен")
    })
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth
    ) {
        Long userId = CurrentUser.id(auth);
        log.info("🔐 POST /api/account/change-password (userId={})", userId);

        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(Map.of("message", "Пароль успешно изменён"));
    }
}
