package com.example.fintrackerpro.entity.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.glassfish.jersey.client.innate.ClientProxy;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные для регистрации нового пользователя")

public class UserRegistrationRequest {
    @Schema(
            description = "Имя пользователя (уникальное)",
            example = "john_doe",
            required = true,
            minLength = 3,
            maxLength = 50
    )
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String userName;

    @Schema(
            description = "Email адрес (уникальный)",
            example = "john@example.com",
            required = true
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    @Schema(
            description = "Пароль (минимум 6 символов)",
            example = "SecurePass123",
            required = true,
            minLength = 6
    )
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    @Schema(
            description = "Telegram Chat ID (опционально)",
            example = "123456789"
    )
    private Long chatId;


}
