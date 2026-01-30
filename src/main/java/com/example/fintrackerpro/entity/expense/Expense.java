package com.example.fintrackerpro.entity.expense;
import com.example.fintrackerpro.entity.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Расход пользователя")

public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор расхода", example = "1")

    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(description = "ID пользователя", example = "5")
    private User user; // расход определенного пользователя

    @Column(name = "amount", nullable = false)
    @Schema(description = "Сумма расхода", example = "1500.50")
    private BigDecimal amount;

    @Column(name = "category", nullable = false)
    @Schema(description = "Категория расхода", example = "Продукты")
    private String category;

    @Column(name = "description")
    @Schema(description = "Описание расхода", example = "Покупка продуктов")
    private String description; // описание расхода

    @Column(name = "date", nullable = false)
    @Schema(description = "Дата расхода", example = "2024-03-15")
    private LocalDateTime date;


    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Дата создания записи", example = "2024-03-15T10:30:00")
    private LocalDateTime createdAt; // создание записи when?


    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
        if (date == null) {
            date = LocalDateTime.now();
        }
    }
}


