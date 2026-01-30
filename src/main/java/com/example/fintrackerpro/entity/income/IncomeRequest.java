package com.example.fintrackerpro.entity.income;

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
@Schema(description = "Данные для создания или обновления дохода")

public class IncomeRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
    @Schema(
            description = "Сумма дохода",
            example = "50000.00",
            required = true
    )
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    @Schema(
            description = "Описание дохода",
            example = "Зарплата за март 2024"
    )
    @NotBlank(message = "Category is required")
    private String category;

    @Schema(
            description = "Источник дохода",
            example = "Зарплата",
            required = true
    )
    @NotBlank(message = "Source is required")
    private String source;
    @Schema(
            description = "Дата получения дохода",
            example = "2024-03-01",
            required = true
    )
    @NotNull(message = "Date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}