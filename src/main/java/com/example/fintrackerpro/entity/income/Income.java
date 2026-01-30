package com.example.fintrackerpro.entity.income;

import com.example.fintrackerpro.entity.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incomes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Доход пользователя")
public class Income {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор дохода", example = "1")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // много доходов принадлежит 1 пользователю
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(description = "ID пользователя", example = "5")
    private User user;

    @Column(name = "amount", nullable = false)
    @Schema(description = "Сумма дохода", example = "50000.00")
    private BigDecimal amount; // доход

    @Column(name = "category")
    @Schema(description = "Описание дохода", example = "Зарплата за март")
    private String category;

    @Column(name = "source")
    @Schema(description = "Источник дохода", example = "Зарплата")
    private String source; // источник

    @Column(name = "date", nullable = false)
    @Schema(description = "Дата получения дохода", example = "2024-03-01")
    private LocalDateTime date;


    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Дата создания записи")
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
        if (date == null) {
            date = LocalDateTime.now();
        }
    }
}
