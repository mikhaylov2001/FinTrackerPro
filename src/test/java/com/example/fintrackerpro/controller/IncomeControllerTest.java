package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.IncomeResponse;
import com.example.fintrackerpro.entity.income.IncomeRequest;
import com.example.fintrackerpro.security.JwtAuthenticationFilter;
import com.example.fintrackerpro.service.IncomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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
@AutoConfigureMockMvc(addFilters = false)
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

        incomeRequest = new IncomeRequest();
        incomeRequest.setUserId(1L);
        incomeRequest.setAmount(new BigDecimal("50000.00"));
        incomeRequest.setSource("Зарплата");
        incomeRequest.setCategory("Зарплата за март");
        incomeRequest.setDate(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("POST /api/incomes - создать доход")
    void createIncome_Success() throws Exception {
        when(incomeService.addIncome(any(IncomeRequest.class))).thenReturn(testIncome);

        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.source").value("Зарплата"))
                .andExpect(jsonPath("$.category").value("Зарплата за март"))
                .andExpect(jsonPath("$.date").value("2024-03-15"));

        verify(incomeService).addIncome(any(IncomeRequest.class));
    }

    @Test
    @DisplayName("GET /api/incomes/{incomeId} - получить доход по ID")
    void getIncome_Success() throws Exception {
        when(incomeService.getIncomeById(1L)).thenReturn(testIncome);

        mockMvc.perform(get("/api/incomes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.source").value("Зарплата"));

        verify(incomeService).getIncomeById(1L);
    }

    @Test
    @DisplayName("GET /api/incomes/{incomeId} - доход не найден")
    void getIncome_NotFound() throws Exception {
        when(incomeService.getIncomeById(999L)).thenThrow(new RuntimeException("Income not found"));

        mockMvc.perform(get("/api/incomes/999"))
                .andExpect(status().is5xxServerError());

        verify(incomeService).getIncomeById(999L);
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

        when(incomeService.updateIncome(eq(1L), any(IncomeRequest.class))).thenReturn(updatedIncome);

        mockMvc.perform(put("/api/incomes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(60000.00));

        verify(incomeService).updateIncome(eq(1L), any(IncomeRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/incomes/{incomeId} - удалить доход")
    void deleteIncome_Success() throws Exception {
        doNothing().when(incomeService).deleteIncome(1L);

        mockMvc.perform(delete("/api/incomes/1"))
                .andExpect(status().isNoContent());

        verify(incomeService).deleteIncome(1L);
    }

    @Disabled("Security filters отключены (addFilters=false), поэтому проверка 401/403 тут невалидна")
    @Test
    @DisplayName("POST /api/incomes - без авторизации")
    void createIncome_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().is4xxClientError());

        verify(incomeService, never()).addIncome(any(IncomeRequest.class));
    }

    @Disabled("Security filters отключены (addFilters=false), поэтому проверка 401/403 тут невалидна")
    @Test
    @DisplayName("GET /api/incomes/{incomeId} - без авторизации")
    void getIncome_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/incomes/1"))
                .andExpect(status().is4xxClientError());

        verify(incomeService, never()).getIncomeById(anyLong());
    }
}
