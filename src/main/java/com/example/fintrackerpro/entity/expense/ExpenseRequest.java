package com.example.fintrackerpro.entity.expense;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Данные для создания или обновления расхода")

public class ExpenseRequest {

    @Schema(
            description = "Сумма расхода",
            example = "1500.50",
            required = true
    )
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Schema(
            description = "Категория расхода",
            example = "Продукты",
            required = true
    )
    @NotBlank(message = "Category is required")
    private String category;
    @Schema(
            description = "Описание расхода",
            example = "Покупка продуктов в магазине"
    )
    @NotBlank(message = "Description is required")
    private String description;
    @Schema(
            description = "Дата расхода",
            example = "2024-03-15",
            required = true
    )
    @NotNull(message = "Date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}
