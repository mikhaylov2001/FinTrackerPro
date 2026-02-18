package com.example.fintrackerpro.dto;// package com.example.fintrackerpro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на смену пароля")
public class ChangePasswordRequest {

    @Schema(description = "Текущий пароль", example = "OldPass123", required = true)
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Schema(description = "Новый пароль", example = "NewStrongPass123", required = true)
    @NotBlank(message = "New password is required")
    private String newPassword;
}
