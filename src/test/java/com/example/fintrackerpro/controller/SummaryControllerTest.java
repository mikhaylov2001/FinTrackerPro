package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.MonthlySummaryDto;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.security.JwtAuthenticationFilter;
import com.example.fintrackerpro.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(
        controllers = SummaryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SummaryController WebMvc Tests")
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
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - получить сводку")
    void getMonthlySummary_Success() throws Exception {
        when(summaryService.getMonthlySummary(1L, 2024, 3)).thenReturn(monthlySummary);

        mockMvc.perform(get("/api/summary/1/month/2024/3").contentType(MediaType.APPLICATION_JSON))
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
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - пользователь не найден")
    void getMonthlySummary_UserNotFound() throws Exception {
        when(summaryService.getMonthlySummary(999L, 2024, 3))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/summary/999/month/2024/3"))
                .andExpect(status().isNotFound());

        verify(summaryService).getMonthlySummary(999L, 2024, 3);
    }

    @Test
    @DisplayName("GET /api/summary/{userId}/month/{year}/{month} - некорректный месяц")
    void getMonthlySummary_InvalidMonth() throws Exception {
        when(summaryService.getMonthlySummary(1L, 2024, 13))
                .thenThrow(new IllegalArgumentException("Invalid month: 13"));

        mockMvc.perform(get("/api/summary/1/month/2024/13"))
                .andExpect(status().is5xxServerError());

        verify(summaryService).getMonthlySummary(1L, 2024, 13);
    }

    @Disabled("Security filters отключены (addFilters=false), поэтому проверка 401/403 тут невалидна")
    @Test
    @DisplayName("GET /api/summary/... - без авторизации")
    void getMonthlySummary_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/summary/1/month/2024/3"))
                .andExpect(status().is4xxClientError());

        verify(summaryService, never()).getMonthlySummary(anyLong(), anyInt(), anyInt());
    }
}
