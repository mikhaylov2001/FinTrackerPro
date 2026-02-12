package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.ExpenseResponse;
import com.example.fintrackerpro.entity.expense.Expense;
import com.example.fintrackerpro.entity.expense.ExpenseRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Unit Tests")
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ExpenseService expenseService;

    private User user;
    private ExpenseRequest req;
    private Expense entity;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        // userId больше НЕ в request
        req = new ExpenseRequest();
        req.setAmount(new BigDecimal("1500.00"));
        req.setCategory("Продукты");
        req.setDescription("Покупка продуктов");
        req.setDate(LocalDate.of(2024, 3, 15));

        entity = new Expense();
        entity.setId(10L);
        entity.setUser(user);
        entity.setAmount(req.getAmount());
        entity.setCategory(req.getCategory());
        entity.setDescription(req.getDescription());
        entity.setDate(req.getDate().atTime(LocalTime.MIDNIGHT));
    }

    @Test
    @DisplayName("Добавить расход - успешно")
    void addExpense_Success() {
        when(userService.getUserEntityById(1L)).thenReturn(user);
        when(expenseRepository.save(any(Expense.class))).thenReturn(entity);

        ExpenseResponse dto = expenseService.addExpense(1L, req);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 3, 15));

        verify(userService).getUserEntityById(1L);
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Добавить расход - проверка преобразования даты (пишем в БД полночь)")
    void addExpense_DateConversion_Success() {
        when(userService.getUserEntityById(1L)).thenReturn(user);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        when(expenseRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        ExpenseResponse dto = expenseService.addExpense(1L, req);

        Expense saved = captor.getValue();
        assertThat(saved.getDate().toLocalDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(saved.getDate().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("Получить расход по ID - успешно (только свой)")
    void getExpenseById_Success() {
        when(expenseRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(entity));

        ExpenseResponse dto = expenseService.getExpenseById(1L, 10L);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getUserId()).isEqualTo(1L);

        verify(expenseRepository).findByIdAndUserId(10L, 1L);
    }

    @Test
    @DisplayName("Получить расход по ID - не найден (или не принадлежит пользователю)")
    void getExpenseById_NotFound() {
        when(expenseRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Expense not found");

        verify(expenseRepository).findByIdAndUserId(999L, 1L);
    }

    @Test
    @DisplayName("Обновить расход - успешно (только свой)")
    void updateExpense_Success() {
        when(expenseRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(entity));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseRequest upd = new ExpenseRequest();
        upd.setAmount(new BigDecimal("2000.00"));
        upd.setCategory("Продукты");
        upd.setDescription("Апдейт");
        upd.setDate(LocalDate.of(2024, 3, 20));

        ExpenseResponse dto = expenseService.updateExpense(1L, 10L, upd);

        assertThat(dto.getAmount()).isEqualByComparingTo("2000.00");
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 3, 20));

        verify(expenseRepository).findByIdAndUserId(10L, 1L);
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Удалить расход - успешно (только свой)")
    void deleteExpense_Success() {
        when(expenseRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(entity));
        doNothing().when(expenseRepository).delete(any(Expense.class));

        expenseService.deleteExpense(1L, 10L);

        verify(expenseRepository).findByIdAndUserId(10L, 1L);
        verify(expenseRepository).delete(entity);
    }
}
