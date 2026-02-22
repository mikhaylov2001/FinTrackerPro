package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.ChangePasswordRequest;
import com.example.fintrackerpro.dto.UpdateEmailRequest;
import com.example.fintrackerpro.dto.UpdateProfileRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.AuthTokenIssuer;
import com.example.fintrackerpro.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final AuthTokenIssuer authTokenIssuer;

    @Operation(summary = "Сменить пароль текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пароль успешно изменён"),
            @ApiResponse(responseCode = "400", description = "Неверные данные или слабый пароль"),
            @ApiResponse(responseCode = "401", description = "Не авторизован или текущий пароль неверен")
    })
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth
    ) {
        Long userId = CurrentUser.id(auth);
        log.info("🔐 POST /api/account/change-password (userId={})", userId);
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Пароль успешно изменён"));
    }

    @Operation(summary = "Обновить профиль (имя, фамилия)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль обновлён, выданы новые токены"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req,
            Authentication auth,
            HttpServletResponse response
    ) {
        Long userId = CurrentUser.id(auth);
        log.info("✏️ PUT /api/account/profile (userId={})", userId);

        // Обновляем профиль в БД
        User updated = userService.updateProfile(userId, req);

        // Переиздаём токены, чтобы фронт получил новый access + refresh с актуальным user
        return authTokenIssuer.issueTokens(updated, response, HttpStatus.OK);
    }

    @Operation(summary = "Изменить email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email изменён, выданы новые токены"),
            @ApiResponse(responseCode = "401", description = "Не авторизован или неверный пароль"),
            @ApiResponse(responseCode = "409", description = "Email уже занят")
    })
    @PutMapping("/email")
    public ResponseEntity<?> updateEmail(
            @Valid @RequestBody UpdateEmailRequest req,
            Authentication auth,
            HttpServletResponse response
    ) {
        Long userId = CurrentUser.id(auth);
        log.info("📧 PUT /api/account/email (userId={})", userId);

        // Обновляем email в БД (проверяется пароль)
        User updated = userService.changeEmail(userId, req.getNewEmail(), req.getPassword());

        // Переиздаём токены
        return authTokenIssuer.issueTokens(updated, response, HttpStatus.OK);
    }
}
