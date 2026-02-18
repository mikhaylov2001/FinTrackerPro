package com.example.fintrackerpro.entity.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные для регистрации нового пользователя")
public class UserRegistrationRequest {

    @Schema(
            description = "Имя",
            example = "Иван",
            required = true,
            minLength = 1,
            maxLength = 50
    )
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @Schema(
            description = "Фамилия",
            example = "Иванов",
            required = true,
            minLength = 1,
            maxLength = 50
    )
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

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

}
