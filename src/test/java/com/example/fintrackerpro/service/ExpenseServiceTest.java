package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.expense.Expense;
import com.example.fintrackerpro.entity.expense.ExpenseRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    private User testUser;
    private Expense testExpense;
    private ExpenseRequest expenseRequest;

    @BeforeEach
    void setUp() {
        // Создаём тестового пользователя
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");

        // Создаём тестовый расход
        testExpense = new Expense();
        testExpense.setId(1L);
        testExpense.setUser(testUser);
        testExpense.setAmount(new BigDecimal("1500.50"));
        testExpense.setCategory("Продукты");
        testExpense.setDescription("Покупка продуктов");
        testExpense.setDate(LocalDate.of(2024, 3, 15).atTime(LocalTime.MIDNIGHT));
        testExpense.setCreatedAt(LocalDateTime.now());

        // Создаём тестовый запрос
        expenseRequest = new ExpenseRequest();
        expenseRequest.setUserId(1L);
        expenseRequest.setAmount(new BigDecimal("1500.50"));
        expenseRequest.setCategory("Продукты");
        expenseRequest.setDescription("Покупка продуктов");
        expenseRequest.setDate(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("Добавить расход - успешно")
    void addExpense_Success() {
        // Given
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        // When
        Expense result = expenseService.addExpense(expenseRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1500.50"));
        assertThat(result.getCategory()).isEqualTo("Продукты");
        assertThat(result.getUser()).isEqualTo(testUser);

        verify(userService).getUserById(1L);
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Добавить расход - пользователь не найден")
    void addExpense_UserNotFound_ThrowsException() {
        // Given
        when(userService.getUserById(1L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then
        assertThatThrownBy(() -> expenseService.addExpense(expenseRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userService).getUserById(1L);
        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    @DisplayName("Получить расход по ID - успешно")
    void getExpenseById_Success() {
        // Given
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

        // When
        Expense result = expenseService.getExpenseById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("1500.50"));

        verify(expenseRepository).findById(1L);
    }

    @Test
    @DisplayName("Получить расход по ID - не найден")
    void getExpenseById_NotFound_ThrowsException() {
        // Given
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> expenseService.getExpenseById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Expense not found");

        verify(expenseRepository).findById(99L);
    }

    @Test
    @DisplayName("Получить расходы пользователя - успешно")
    void getExpensesByUser_Success() {
        // Given
        List<Expense> expenses = Arrays.asList(testExpense);
        Page<Expense> expensePage = new PageImpl<>(expenses);
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.getUserById(1L)).thenReturn(testUser);
        when(expenseRepository.findByUserId(1L, pageable)).thenReturn(expensePage);

        // When
        Page<Expense> result = expenseService.getExpensesByUser(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testExpense);

        verify(userService).getUserById(1L);
        verify(expenseRepository).findByUserId(1L, pageable);
    }

    @Test
    @DisplayName("Получить расходы за месяц - успешно")
    void getExpensesByUserAndMonth_Success() {
        // Given
        List<Expense> expenses = Arrays.asList(testExpense);
        Page<Expense> expensePage = new PageImpl<>(expenses);
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.getUserById(1L)).thenReturn(testUser);
        when(expenseRepository.findByUserIdAndYearAndMonth(1L, 2024, 3, pageable))
                .thenReturn(expensePage);

        // When
        Page<Expense> result = expenseService.getExpensesByUserAndMonth(1L, 2024, 3, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(userService).getUserById(1L);
        verify(expenseRepository).findByUserIdAndYearAndMonth(1L, 2024, 3, pageable);
    }

    @Test
    @DisplayName("Обновить расход - успешно")
    void updateExpense_Success() {
        // Given
        ExpenseRequest updateRequest = new ExpenseRequest();
        updateRequest.setAmount(new BigDecimal("2000.00"));
        updateRequest.setCategory("Транспорт");
        updateRequest.setDescription("Бензин");
        updateRequest.setDate(LocalDate.of(2024, 3, 20));

        Expense updatedExpense = new Expense();
        updatedExpense.setId(1L);
        updatedExpense.setUser(testUser);
        updatedExpense.setAmount(new BigDecimal("2000.00"));
        updatedExpense.setCategory("Транспорт");
        updatedExpense.setDescription("Бензин");
        updatedExpense.setDate(LocalDate.of(2024, 3, 20).atTime(LocalTime.MIDNIGHT));

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(updatedExpense);

        // When
        Expense result = expenseService.updateExpense(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.getCategory()).isEqualTo("Транспорт");
        assertThat(result.getDescription()).isEqualTo("Бензин");

        verify(expenseRepository).findById(1L);
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Обновить расход - частичное обновление")
    void updateExpense_PartialUpdate_Success() {
        // Given
        ExpenseRequest partialRequest = new ExpenseRequest();
        partialRequest.setAmount(new BigDecimal("2500.00"));
        // Остальные поля null - не обновляются

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Expense result = expenseService.updateExpense(1L, partialRequest);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));
        // Старые значения сохранились
        assertThat(result.getCategory()).isEqualTo("Продукты");
        assertThat(result.getDescription()).isEqualTo("Покупка продуктов");

        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Удалить расход - успешно")
    void deleteExpense_Success() {
        // Given
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
        doNothing().when(expenseRepository).delete(any(Expense.class));

        // When
        expenseService.deleteExpense(1L);

        // Then
        verify(expenseRepository).findById(1L);
        verify(expenseRepository).delete(testExpense);
    }

    @Test
    @DisplayName("Удалить расход - не найден")
    void deleteExpense_NotFound_ThrowsException() {
        // Given
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> expenseService.deleteExpense(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Expense not found");

        verify(expenseRepository).findById(99L);
        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    @Test
    @DisplayName("Добавить расход - проверка преобразования даты в LocalDateTime")
    void addExpense_DateConversion_Success() {
        // Given
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Expense result = expenseService.addExpense(expenseRequest);

        // Then
        assertThat(result.getDate()).isNotNull();
        assertThat(result.getDate().toLocalDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(result.getDate().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);

        verify(expenseRepository).save(any(Expense.class));
    }
}