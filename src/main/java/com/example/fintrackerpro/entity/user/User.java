package com.example.fintrackerpro.entity.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Пользователь системы")
public class User extends UserDto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор пользователя", example = "1")
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = true)
    @Schema(description = "Telegram Chat ID")
    private Long chatId;  // телеграм чат

    @Column(unique = true)
    @Schema(description = "Имя пользователя")
    private String userName;

    @Column(name = "email", unique = true, nullable = false)
    @NotBlank(message = "Email не может быть пусто")
    @Email(message = "Email должен быть валидным")
    @Schema(description = "Email адрес", example = "john@example.com")
    private String email;

    @Column(name = "password")
    @NotBlank
    @Schema(description = "Хешированный пароль (скрыт в ответах)")
    private String password;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Дата создания аккаунта", example = "2024-03-01T10:00:00")
    private LocalDateTime createdAt; // дата создания (не меняется)

    @Column(name = "updated_at")
    @Schema(description = "Дата последнего обновления", example = "2024-03-15T14:30:00")
    private LocalDateTime updatedAt; // дата последнего обновления

    @Schema(description = "Google ID (если регистрация через Google)")
    private String googleId;

    // Геттеры и сеттеры
    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }





    @PrePersist
    public void prePersist() {  // created
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }



}
