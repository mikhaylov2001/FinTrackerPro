package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.MonthlySummaryDto;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SummaryController Integration Tests")
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummaryService summaryService;

    private MonthlySummaryDto monthlySummary;

    @BeforeEach
    void setUp() {
        monthlySummary = MonthlySummaryDto.builder()
                .year(2024)
                .month(3)
                .totalIncome(new BigDecimal("60000.00"))
                .totalExpenses(new BigDecimal("15000.00"))
                .savings(new BigDecimal("45000.00"))
                .balance(new BigDecimal("45000.00"))
                .savingsRatePercent(new BigDecimal("75.00"))
                .build();
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - получить сводку")
    void getMonthlySummary_Success() throws Exception {
        // Given
        when(summaryService.getMonthlySummary(1L, 2024, 3))
                .thenReturn(monthlySummary);

        // When & Then
        mockMvc.perform(get("/api/summary/1/month/2024/3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.month").value(3))
                .andExpect(jsonPath("$.total_income").value(60000.00))
                .andExpect(jsonPath("$.total_expenses").value(15000.00))
                .andExpect(jsonPath("$.balance").value(45000.00))
                .andExpect(jsonPath("$.savings").value(45000.00))
                .andExpect(jsonPath("$.savings_rate_percent").value(75.00));

        verify(summaryService).getMonthlySummary(1L, 2024, 3);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - для другого месяца")
    void getMonthlySummary_DifferentMonth_Success() throws Exception {
        // Given
        MonthlySummaryDto septemberSummary = MonthlySummaryDto.builder()
                .year(2024)
                .month(9)
                .totalIncome(new BigDecimal("50000.00"))
                .totalExpenses(new BigDecimal("20000.00"))
                .savings(new BigDecimal("30000.00"))
                .balance(new BigDecimal("30000.00"))
                .savingsRatePercent(new BigDecimal("60.00"))
                .build();

        when(summaryService.getMonthlySummary(1L, 2024, 9))
                .thenReturn(septemberSummary);

        // When & Then
        mockMvc.perform(get("/api/summary/1/month/2024/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.total_income").value(50000.00))
                .andExpect(jsonPath("$.total_expenses").value(20000.00))
                .andExpect(jsonPath("$.balance").value(30000.00));

        verify(summaryService).getMonthlySummary(1L, 2024, 9);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - месяц без данных (нули)")
    void getMonthlySummary_NoData_ReturnsZeros() throws Exception {
        // Given
        MonthlySummaryDto emptySummary = MonthlySummaryDto.builder()
                .year(2024)
                .month(12)
                .totalIncome(BigDecimal.ZERO)
                .totalExpenses(BigDecimal.ZERO)
                .savings(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .savingsRatePercent(BigDecimal.ZERO)
                .build();

        when(summaryService.getMonthlySummary(1L, 2024, 12))
                .thenReturn(emptySummary);

        // When & Then
        mockMvc.perform(get("/api/summary/1/month/2024/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.month").value(12))
                .andExpect(jsonPath("$.total_income").value(0))
                .andExpect(jsonPath("$.total_expenses").value(0))
                .andExpect(jsonPath("$.balance").value(0));

        verify(summaryService).getMonthlySummary(1L, 2024, 12);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - пользователь не найден")
    void getMonthlySummary_UserNotFound() throws Exception {
        // Given
        when(summaryService.getMonthlySummary(999L, 2024, 3))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        // When & Then
        mockMvc.perform(get("/api/summary/999/month/2024/3"))
                .andExpect(status().isNotFound());

        verify(summaryService).getMonthlySummary(999L, 2024, 3);
    }

    @Test
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - без авторизации возвращает 401")
    void getMonthlySummary_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/summary/1/month/2024/3"))
                .andExpect(status().isUnauthorized());

        verify(summaryService, never()).getMonthlySummary(anyLong(), anyInt(), anyInt());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - отрицательный баланс")
    void getMonthlySummary_NegativeBalance() throws Exception {
        // Given - расходы больше доходов
        MonthlySummaryDto negativeSummary = MonthlySummaryDto.builder()
                .year(2024)
                .month(6)
                .totalIncome(new BigDecimal("30000.00"))
                .totalExpenses(new BigDecimal("50000.00"))
                .savings(new BigDecimal("-20000.00"))
                .balance(new BigDecimal("-20000.00"))
                .savingsRatePercent(new BigDecimal("-66.67"))
                .build();

        when(summaryService.getMonthlySummary(1L, 2024, 6))
                .thenReturn(negativeSummary);

        // When & Then
        mockMvc.perform(get("/api/summary/1/month/2024/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_income").value(30000.00))
                .andExpect(jsonPath("$.total_expenses").value(50000.00))
                .andExpect(jsonPath("$.balance").value(-20000.00))
                .andExpect(jsonPath("$.savings").value(-20000.00));

        verify(summaryService).getMonthlySummary(1L, 2024, 6);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - некорректный месяц")
    void getMonthlySummary_InvalidMonth() throws Exception {
        // Given
        when(summaryService.getMonthlySummary(1L, 2024, 13))
                .thenThrow(new IllegalArgumentException("Invalid month: 13"));

        // When & Then
        mockMvc.perform(get("/api/summary/1/month/2024/13"))
                .andExpect(status().isInternalServerError());

        verify(summaryService).getMonthlySummary(1L, 2024, 13);
    }
}
