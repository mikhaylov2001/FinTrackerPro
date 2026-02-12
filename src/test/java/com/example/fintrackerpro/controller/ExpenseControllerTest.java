package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.ExpenseResponse;
import com.example.fintrackerpro.entity.expense.ExpenseRequest;
import com.example.fintrackerpro.security.JwtAuthenticationFilter;
import com.example.fintrackerpro.service.ExpenseService;
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
        controllers = ExpenseController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc // ВАЖНО: фильтры включены
@DisplayName("ExpenseController WebMvc Tests")
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    private ExpenseResponse testExpense;
    private ExpenseRequest expenseRequest;

    private Authentication authUser1() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @BeforeEach
    void setUp() {
        testExpense = ExpenseResponse.builder()
                .id(1L)
                .userId(1L)
                .amount(new BigDecimal("1500.00"))
                .category("Продукты")
                .description("Покупка продуктов")
                .date(LocalDate.of(2024, 3, 15))
                .build();

        // userId НЕ передаём в body
        expenseRequest = new ExpenseRequest();
        expenseRequest.setAmount(new BigDecimal("1500.00"));
        expenseRequest.setCategory("Продукты");
        expenseRequest.setDescription("Покупка продуктов");
        expenseRequest.setDate(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("POST /api/expenses - создать расход")
    void createExpense_Success() throws Exception {
        when(expenseService.addExpense(eq(1L), any(ExpenseRequest.class))).thenReturn(testExpense);

        mockMvc.perform(post("/api/expenses")
                        .with(authentication(authUser1()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.category").value("Продукты"))
                .andExpect(jsonPath("$.description").value("Покупка продуктов"))
                .andExpect(jsonPath("$.date").value("2024-03-15"))
                .andExpect(jsonPath("$.amount").value(1500.00));

        verify(expenseService).addExpense(eq(1L), any(ExpenseRequest.class));
    }

    @Test
    @DisplayName("GET /api/expenses/{expenseId} - получить расход по ID")
    void getExpense_Success() throws Exception {
        when(expenseService.getExpenseById(1L, 1L)).thenReturn(testExpense);

        mockMvc.perform(get("/api/expenses/1")
                        .with(authentication(authUser1())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.amount").value(1500.00));

        verify(expenseService).getExpenseById(1L, 1L);
    }

    @Test
    @DisplayName("PUT /api/expenses/{expenseId} - обновить расход")
    void updateExpense_Success() throws Exception {
        when(expenseService.updateExpense(eq(1L), eq(1L), any(ExpenseRequest.class))).thenReturn(testExpense);

        mockMvc.perform(put("/api/expenses/1")
                        .with(authentication(authUser1()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(expenseService).updateExpense(eq(1L), eq(1L), any(ExpenseRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/expenses/{expenseId} - удалить расход")
    void deleteExpense_Success() throws Exception {
        doNothing().when(expenseService).deleteExpense(1L, 1L);

        mockMvc.perform(delete("/api/expenses/1")
                        .with(authentication(authUser1()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(expenseService).deleteExpense(1L, 1L);
    }
}
