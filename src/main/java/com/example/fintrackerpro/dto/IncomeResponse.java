package com.example.fintrackerpro.dto;

import com.example.fintrackerpro.entity.income.Income;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeResponse {
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private String category;
    private String source;
    private LocalDate date;

    public static IncomeResponse from(Income x) {
        return IncomeResponse.builder()
                .id(x.getId())
                .userId(x.getUser() != null ? x.getUser().getId() : null)
                .amount(x.getAmount())
                .category(x.getCategory())
                .source(x.getSource())
                .date(x.getDate() != null ? x.getDate().toLocalDate() : null) // у тебя LocalDateTime в entity
                .build();
    }
}
