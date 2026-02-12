package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.IncomeResponse;
import com.example.fintrackerpro.entity.income.IncomeRequest;
import com.example.fintrackerpro.security.JwtAuthenticationFilter;
import com.example.fintrackerpro.service.IncomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(
        controllers = IncomeController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc // ВАЖНО: фильтры включены (не ставим addFilters=false)
@DisplayName("IncomeController WebMvc Tests")
class IncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncomeService incomeService;

    private IncomeResponse testIncome;
    private IncomeRequest incomeRequest;

    private Authentication authUser1() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @BeforeEach
    void setUp() {
        testIncome = IncomeResponse.builder()
                .id(1L)
                .userId(1L)
                .amount(new BigDecimal("50000.00"))
                .source("Зарплата")
                .category("Зарплата за март")
                .date(LocalDate.of(2024, 3, 15))
                .build();

        // userId НЕ передаём в body
        incomeRequest = new IncomeRequest();
        incomeRequest.setAmount(new BigDecimal("50000.00"));
        incomeRequest.setSource("Зарплата");
        incomeRequest.setCategory("Зарплата за март");
        incomeRequest.setDate(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("POST /api/incomes - создать доход")
    void createIncome_Success() throws Exception {
        when(incomeService.addIncome(eq(1L), any(IncomeRequest.class))).thenReturn(testIncome);

        mockMvc.perform(post("/api/incomes")
                        .with(authentication(authUser1()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.source").value("Зарплата"))
                .andExpect(jsonPath("$.category").value("Зарплата за март"))
                .andExpect(jsonPath("$.date").value("2024-03-15"));

        verify(incomeService).addIncome(eq(1L), any(IncomeRequest.class));
    }

    @Test
    @DisplayName("GET /api/incomes/{incomeId} - получить доход по ID")
    void getIncome_Success() throws Exception {
        when(incomeService.getIncomeById(1L, 1L)).thenReturn(testIncome);

        mockMvc.perform(get("/api/incomes/1")
                        .with(authentication(authUser1())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.source").value("Зарплата"));

        verify(incomeService).getIncomeById(1L, 1L);
    }

    @Test
    @DisplayName("PUT /api/incomes/{incomeId} - обновить доход")
    void updateIncome_Success() throws Exception {
        IncomeResponse updatedIncome = IncomeResponse.builder()
                .id(1L)
                .userId(1L)
                .amount(new BigDecimal("60000.00"))
                .source("Годовая премия")
                .category("Премия")
                .date(LocalDate.of(2024, 3, 20))
                .build();

        when(incomeService.updateIncome(eq(1L), eq(1L), any(IncomeRequest.class))).thenReturn(updatedIncome);

        mockMvc.perform(put("/api/incomes/1")
                        .with(authentication(authUser1()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(60000.00));

        verify(incomeService).updateIncome(eq(1L), eq(1L), any(IncomeRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/incomes/{incomeId} - удалить доход")
    void deleteIncome_Success() throws Exception {
        doNothing().when(incomeService).deleteIncome(1L, 1L);

        mockMvc.perform(delete("/api/incomes/1")
                        .with(authentication(authUser1()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(incomeService).deleteIncome(1L, 1L);
    }
}
