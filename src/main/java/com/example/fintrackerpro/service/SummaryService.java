package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.MonthlySummaryDto;
import com.example.fintrackerpro.repository.ExpenseRepository;
import com.example.fintrackerpro.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final UserService userService;


    //  Получить полный summary за месяц

    public MonthlySummaryDto getMonthlySummary(Long userId, int year, int month) {

        // Проверяем что пользователь существует
        userService.getUserById(userId);

        BigDecimal totalIncome = incomeRepository.getTotalIncomeByUserAndMonth(userId, year, month);
        BigDecimal totalExpenses = expenseRepository.getTotalExpenseByUserAndMonth(userId, year, month);

        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal savings = totalIncome.subtract(totalExpenses);

        // Рассчитываем норму сбережений (в %)
        BigDecimal savingsRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = savings
                    .divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        log.info("📊 Monthly summary for user {} {}/{}: income={}, expense={}, savings={}, rate={}%",
                userId, year, month, totalIncome, totalExpenses, savings, savingsRate);

        return MonthlySummaryDto.builder()
                .year(year)
                .month(month)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .savings(savings)
                .savingsRatePercent(savingsRate)
                .balance(savings) // Balance = savings
                .build();
    }

    public BigDecimal getTotalIncome(Long userId, int year, int month) {
        BigDecimal total = incomeRepository.getTotalIncomeByUserAndMonth(userId, year, month);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getTotalExpense(Long userId, int year, int month) {
        BigDecimal total = expenseRepository.getTotalExpenseByUserAndMonth(userId, year, month);
        return total != null ? total : BigDecimal.ZERO;
    }

//    public BigDecimal getBalance(Long userId, int year, int month) {
//        BigDecimal income = getTotalIncome(userId, year, month);
//        BigDecimal expense = getTotalExpense(userId, year, month);
//        return income.subtract(expense);
//    }
}
