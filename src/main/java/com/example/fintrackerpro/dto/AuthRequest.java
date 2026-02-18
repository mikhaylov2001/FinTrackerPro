package com.example.fintrackerpro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Данные для входа пользователя")
public class AuthRequest {

    @Schema(
            description = "Имя пользователя",
            example = "john_doe",
            required = true
    )
    @Email(message = "Email должен быть валидным")
    @NotBlank(message = "Email is required")
    private String email;
    @Schema(
            description = "Пароль",
            example = "SecurePass123",
            required = true
    )
    @NotBlank(message = "Password is required")
    private String password;

}

