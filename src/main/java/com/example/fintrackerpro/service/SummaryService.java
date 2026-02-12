package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.MonthlySummaryDto;
import com.example.fintrackerpro.repository.ExpenseRepository;
import com.example.fintrackerpro.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

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
        userService.getUserEntityById(userId);

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

    public List<MonthlySummaryDto> getAllMonthlySummaries(Long userId) {
        List<String> months = getUsedMonths(userId);
        List<MonthlySummaryDto> result = new ArrayList<>();

        for (String monthStr : months) {
            String[] parts = monthStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            MonthlySummaryDto summary = getMonthlySummary(userId, year, month);
            result.add(summary);
        }

        return result;
    }

    public List<String> getUsedMonths(Long userId) {
        // TreeSet с обратным порядком — новые месяцы сначала
        Set<String> months = new TreeSet<>(Collections.reverseOrder());

        // Месяцы из доходов
        List<Object[]> incomeMonths = incomeRepository.findUsedMonthsByUser(userId);
        for (Object[] row : incomeMonths) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            months.add(String.format("%04d-%02d", year, month)); // "2026-02"
        }

        // Месяцы из расходов
        List<Object[]> expenseMonths = expenseRepository.findUsedMonthsByUser(userId);
        for (Object[] row : expenseMonths) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            months.add(String.format("%04d-%02d", year, month));
        }

        return new ArrayList<>(months);
    }
}


