package com.example.fintrackerpro.dto;

import com.example.fintrackerpro.entity.expense.Expense;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Ответ API: расход")
public class ExpenseResponse {
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDate date;

    public static ExpenseResponse from(Expense x) {
        return ExpenseResponse.builder()
                .id(x.getId())
                .userId(x.getUser() != null ? x.getUser().getId() : null)
                .amount(x.getAmount())
                .category(x.getCategory())
                .description(x.getDescription())
                .date(LocalDate.from(x.getDate()))
                .build();
    }
}
