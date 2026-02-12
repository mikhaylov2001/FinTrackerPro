package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.MonthlySummaryDto;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.repository.ExpenseRepository;
import com.example.fintrackerpro.repository.IncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryService Unit Tests")
class SummaryServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private SummaryService summaryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Получить сводку за месяц - успешно")
    void getMonthlySummary_Success() {
        // Given
        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(expenseRepository.getTotalExpenseByUserAndMonth(1L, 2024, 3))
                .thenReturn(new BigDecimal("1500.00"));
        when(incomeRepository.getTotalIncomeByUserAndMonth(1L, 2024, 3))
                .thenReturn(new BigDecimal("60000.00"));

        // When
        MonthlySummaryDto result = summaryService.getMonthlySummary(1L, 2024, 3);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(result.getTotalIncome()).isEqualByComparingTo(new BigDecimal("60000.00"));
        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("58500.00"));
        assertThat(result.getSavings()).isEqualByComparingTo(new BigDecimal("58500.00"));
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonth()).isEqualTo(3);

        verify(userService).getUserEntityById(1L);
        verify(expenseRepository).getTotalExpenseByUserAndMonth(1L, 2024, 3);
        verify(incomeRepository).getTotalIncomeByUserAndMonth(1L, 2024, 3);
    }

    @Test
    @DisplayName("Сводка за месяц - нет данных (null от репозиториев)")
    void getMonthlySummary_NoData_ReturnsZeros() {
        // Given
        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(expenseRepository.getTotalExpenseByUserAndMonth(1L, 2024, 3))
                .thenReturn(null);
        when(incomeRepository.getTotalIncomeByUserAndMonth(1L, 2024, 3))
                .thenReturn(null);

        // When
        MonthlySummaryDto result = summaryService.getMonthlySummary(1L, 2024, 3);

        // Then
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getSavingsRatePercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Сводка за месяц - только расходы")
    void getMonthlySummary_OnlyExpenses() {
        // Given
        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(expenseRepository.getTotalExpenseByUserAndMonth(1L, 2024, 3))
                .thenReturn(new BigDecimal("2000"));
        when(incomeRepository.getTotalIncomeByUserAndMonth(1L, 2024, 3))
                .thenReturn(null);

        // When
        MonthlySummaryDto result = summaryService.getMonthlySummary(1L, 2024, 3);

        // Then
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("2000"));
        assertThat(result.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("-2000"));
        assertThat(result.getSavings()).isEqualByComparingTo(new BigDecimal("-2000"));
    }

    @Test
    @DisplayName("Сводка за месяц - только доходы")
    void getMonthlySummary_OnlyIncome() {
        // Given
        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(expenseRepository.getTotalExpenseByUserAndMonth(1L, 2024, 3))
                .thenReturn(null);
        when(incomeRepository.getTotalIncomeByUserAndMonth(1L, 2024, 3))
                .thenReturn(new BigDecimal("30000"));

        // When
        MonthlySummaryDto result = summaryService.getMonthlySummary(1L, 2024, 3);

        // Then
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalIncome()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.getSavings()).isEqualByComparingTo(new BigDecimal("30000"));
        // Savings rate = 100%
        assertThat(result.getSavingsRatePercent()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Расчёт процента сбережений - корректный")
    void getMonthlySummary_SavingsRateCalculation() {
        // Given
        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(incomeRepository.getTotalIncomeByUserAndMonth(1L, 2024, 3))
                .thenReturn(new BigDecimal("100000"));
        when(expenseRepository.getTotalExpenseByUserAndMonth(1L, 2024, 3))
                .thenReturn(new BigDecimal("70000"));

        // When
        MonthlySummaryDto result = summaryService.getMonthlySummary(1L, 2024, 3);

        // Then
        // Savings = 100000 - 70000 = 30000
        // Rate = (30000 / 100000) * 100 = 30%
        assertThat(result.getSavings()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.getSavingsRatePercent()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("getTotalIncome - успешно")
    void getTotalIncome_Success() {
        // Given
        when(incomeRepository.getTotalIncomeByUserAndMonth(1L, 2024, 3))
                .thenReturn(new BigDecimal("50000"));

        // When
        BigDecimal result = summaryService.getTotalIncome(1L, 2024, 3);

        // Then
        assertThat(result).isEqualByComparingTo(new BigDecimal("50000"));
        verify(incomeRepository).getTotalIncomeByUserAndMonth(1L, 2024, 3);
    }

    @Test
    @DisplayName("getTotalIncome - null от репозитория")
    void getTotalIncome_NullFromRepository() {
        // Given
        when(incomeRepository.getTotalIncomeByUserAndMonth(1L, 2024, 3))
                .thenReturn(null);

        // When
        BigDecimal result = summaryService.getTotalIncome(1L, 2024, 3);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getTotalExpense - успешно")
    void getTotalExpense_Success() {
        // Given
        when(expenseRepository.getTotalExpenseByUserAndMonth(1L, 2024, 3))
                .thenReturn(new BigDecimal("15000"));

        // When
        BigDecimal result = summaryService.getTotalExpense(1L, 2024, 3);

        // Then
        assertThat(result).isEqualByComparingTo(new BigDecimal("15000"));
        verify(expenseRepository).getTotalExpenseByUserAndMonth(1L, 2024, 3);
    }

    @Test
    @DisplayName("getTotalExpense - null от репозитория")
    void getTotalExpense_NullFromRepository() {
        // Given
        when(expenseRepository.getTotalExpenseByUserAndMonth(1L, 2024, 3))
                .thenReturn(null);

        // When
        BigDecimal result = summaryService.getTotalExpense(1L, 2024, 3);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
