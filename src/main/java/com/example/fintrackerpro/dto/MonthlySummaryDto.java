package com.example.fintrackerpro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySummaryDto {
    
    @JsonProperty("year")
    private int year;
    
    @JsonProperty("month")
    private int month;
    
    @JsonProperty("total_income")
    private BigDecimal totalIncome;
    
    @JsonProperty("total_expenses")
    private BigDecimal totalExpenses;
    
    @JsonProperty("savings")
    private BigDecimal savings;
    
    @JsonProperty("savings_rate_percent")
    private BigDecimal savingsRatePercent;
    
    @JsonProperty("balance")
    private BigDecimal balance;
}
