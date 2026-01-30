package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.entity.income.Income;
import com.example.fintrackerpro.entity.income.IncomeRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.service.IncomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IncomeController Integration Tests")
class IncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IncomeService incomeService;

    private User testUser;
    private Income testIncome;
    private IncomeRequest incomeRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");

        testIncome = new Income();
        testIncome.setId(1L);
        testIncome.setUser(testUser);
        testIncome.setAmount(new BigDecimal("50000.00"));
        testIncome.setCategory("Доход");
        testIncome.setSource("Зарплата");
        testIncome.setCategory("Зарплата за март");
        testIncome.setDate(LocalDate.of(2024, 3, 15).atTime(LocalTime.MIDNIGHT));

        incomeRequest = new IncomeRequest();
        incomeRequest.setUserId(1L);
        incomeRequest.setAmount(new BigDecimal("50000.00"));
        incomeRequest.setCategory("Доход");
        incomeRequest.setSource("Зарплата");
        incomeRequest.setCategory("Зарплата за март");
        incomeRequest.setDate(LocalDate.of(2024, 3, 15));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/incomes - создать доход")
    void createIncome_Success() throws Exception {
        // Given
        when(incomeService.addIncome(any(IncomeRequest.class)))
                .thenReturn(testIncome);

        // When & Then
        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.source").value("Зарплата"));

        verify(incomeService).addIncome(any(IncomeRequest.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/incomes/{incomeId} - получить доход по ID")
    void getIncome_Success() throws Exception {
        // Given
        when(incomeService.getIncomeById(1L)).thenReturn(testIncome);

        // When & Then
        mockMvc.perform(get("/api/incomes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.source").value("Зарплата"));

        verify(incomeService).getIncomeById(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/incomes/{incomeId} - доход не найден")
    void getIncome_NotFound() throws Exception {
        // Given
        when(incomeService.getIncomeById(999L))
                .thenThrow(new RuntimeException("Income not found"));

        // When & Then
        mockMvc.perform(get("/api/incomes/999"))
                .andExpect(status().isInternalServerError());

        verify(incomeService).getIncomeById(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/incomes/{incomeId} - обновить доход")
    void updateIncome_Success() throws Exception {
        // Given
        Income updatedIncome = new Income();
        updatedIncome.setId(1L);
        updatedIncome.setUser(testUser);
        updatedIncome.setAmount(new BigDecimal("60000.00"));
        updatedIncome.setCategory("Премия");
        updatedIncome.setSource("Годовая премия");
        updatedIncome.setDate(LocalDate.of(2024, 3, 20).atTime(LocalTime.MIDNIGHT));

        when(incomeService.updateIncome(eq(1L), any(IncomeRequest.class)))
                .thenReturn(updatedIncome);

        // When & Then
        mockMvc.perform(put("/api/incomes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(60000.00));

        verify(incomeService).updateIncome(eq(1L), any(IncomeRequest.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/incomes/{incomeId} - удалить доход")
    void deleteIncome_Success() throws Exception {
        // Given
        doNothing().when(incomeService).deleteIncome(1L);

        // When & Then
        mockMvc.perform(delete("/api/incomes/1"))
                .andExpect(status().isNoContent());

        verify(incomeService).deleteIncome(1L);
    }

    @Test
    @DisplayName("POST /api/incomes - без авторизации возвращает 401")
    void createIncome_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeRequest)))
                .andExpect(status().isUnauthorized());

        verify(incomeService, never()).addIncome(any(IncomeRequest.class));
    }

    @Test
    @DisplayName("GET /api/incomes/{incomeId} - без авторизации возвращает 401")
    void getIncome_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/incomes/1"))
                .andExpect(status().isUnauthorized());

        verify(incomeService, never()).getIncomeById(anyLong());
    }
}
