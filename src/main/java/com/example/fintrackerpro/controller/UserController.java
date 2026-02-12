package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserDto;
import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "API для управления пользователями")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Получить пользователя по ID (сам пользователь или админ)",
            description = "Возвращает информацию о пользователе по его идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long userId, Authentication auth) {
        Long current = CurrentUser.id(auth);
        if (!current.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        log.info("📤 GET /api/users/{} (currentUser={})", userId, current);
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @Operation(
            summary = "Получить профиль текущего пользователя",
            description = "Возвращает информацию о текущем аутентифицированном пользователе"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Профиль пользователя получен",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(Authentication auth) {
        Long current = CurrentUser.id(auth);
        log.info("📤 GET /api/users/me (userId={})", current);
        return ResponseEntity.ok(userService.getUserById(current));
    }

    @Operation(
            summary = "Получить всех пользователей (опционально: только для админа)",
            description = "Возвращает список всех пользователей"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список пользователей получен"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers(Authentication auth) {
        Long current = CurrentUser.id(auth);
        log.info("📤 GET /api/users (requestedBy={})", current);
        // при появлении ролей можно проверять ADMIN здесь
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(
            summary = "Обновить профиль пользователя",
            description = "Обновляет данные текущего пользователя (имя, email)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody User updatedUser,
            Authentication auth
    ) {
        Long current = CurrentUser.id(auth);
        if (!current.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        log.info("🔄 PUT /api/users/{} (currentUser={})", userId, current);
        return ResponseEntity.ok(userService.updateUser(userId, updatedUser));
    }

    @Operation(
            summary = "Удалить аккаунт",
            description = "Удаляет аккаунт текущего пользователя и все связанные данные"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Аккаунт удалён"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId, Authentication auth) {
        Long current = CurrentUser.id(auth);
        if (!current.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        log.info("🗑️ DELETE /api/users/{} (currentUser={})", userId, current);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
