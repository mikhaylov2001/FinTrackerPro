package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.MonthlySummaryDto;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.security.JwtAuthenticationFilter;
import com.example.fintrackerpro.service.SummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
@AutoConfigureMockMvc
@DisplayName("SummaryController WebMvc Tests")
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SummaryService summaryService;

    private MonthlySummaryDto monthlySummary;

    private Authentication authUser1() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

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
    @DisplayName("GET /api/summary/me/month/{year}/{month} - получить сводку")
    void getMonthlySummary_Success() throws Exception {
        when(summaryService.getMonthlySummary(1L, 2024, 3)).thenReturn(monthlySummary);

        mockMvc.perform(get("/api/summary/me/month/2024/3")
                        .with(authentication(authUser1()))
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
    @DisplayName("GET /api/summary/me/month/{year}/{month} - пользователь не найден")
    void getMonthlySummary_UserNotFound() throws Exception {
        when(summaryService.getMonthlySummary(1L, 2024, 3))
                .thenThrow(new ResourceNotFoundException("User not found with id: 1"));

        mockMvc.perform(get("/api/summary/me/month/2024/3")
                        .with(authentication(authUser1())))
                .andExpect(status().isNotFound());

        verify(summaryService).getMonthlySummary(1L, 2024, 3);
    }
}
