package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.IncomeResponse;
import com.example.fintrackerpro.entity.income.Income;
import com.example.fintrackerpro.entity.income.IncomeRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.repository.IncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncomeService Unit Tests")
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private IncomeService incomeService;

    private User testUser;
    private Income testIncome;
    private IncomeRequest incomeRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");

        testIncome = new Income();
        testIncome.setId(1L);
        testIncome.setUser(testUser);
        testIncome.setAmount(new BigDecimal("50000.00"));
        testIncome.setSource("Зарплата");
        testIncome.setCategory("Зарплата за март");
        testIncome.setDate(LocalDate.of(2024, 3, 15).atTime(LocalTime.MIDNIGHT));
        testIncome.setCreatedAt(LocalDateTime.now());

        // userId больше НЕ хранится в request
        incomeRequest = new IncomeRequest();
        incomeRequest.setAmount(new BigDecimal("50000.00"));
        incomeRequest.setSource("Зарплата");
        incomeRequest.setCategory("Зарплата за март");
        incomeRequest.setDate(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("Добавить доход - успешно")
    void addIncome_Success() {
        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(incomeRepository.save(any(Income.class))).thenReturn(testIncome);

        IncomeResponse result = incomeService.addIncome(1L, incomeRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(result.getSource()).isEqualTo("Зарплата");
        assertThat(result.getCategory()).isEqualTo("Зарплата за март");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 3, 15));

        verify(userService).getUserEntityById(1L);
        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    @DisplayName("Добавить доход - пользователь не найден")
    void addIncome_UserNotFound_ThrowsException() {
        when(userService.getUserEntityById(1L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertThatThrownBy(() -> incomeService.addIncome(1L, incomeRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userService).getUserEntityById(1L);
        verify(incomeRepository, never()).save(any(Income.class));
    }

    @Test
    @DisplayName("Получить доход по ID - успешно (только свой)")
    void getIncomeById_Success() {
        when(incomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testIncome));

        IncomeResponse result = incomeService.getIncomeById(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));

        verify(incomeRepository).findByIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("Получить доход по ID - не найден (или не принадлежит пользователю)")
    void getIncomeById_NotFound_ThrowsException() {
        when(incomeRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.getIncomeById(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Income not found");

        verify(incomeRepository).findByIdAndUserId(99L, 1L);
    }

    @Test
    @DisplayName("Получить доходы пользователя - успешно (с Pageable)")
    void getIncomesByUser_WithPageable_Success() {
        Page<Income> incomePage = new PageImpl<>(Arrays.asList(testIncome));
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(incomeRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(incomePage);

        Page<IncomeResponse> result = incomeService.getIncomesByUser(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        IncomeResponse dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo(1L);

        verify(userService).getUserEntityById(1L);
        verify(incomeRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }

    @Test
    @DisplayName("Получить доходы за месяц - успешно")
    void getIncomesByUserAndMonth_Success() {
        Page<Income> incomePage = new PageImpl<>(Arrays.asList(testIncome));
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.getUserEntityById(1L)).thenReturn(testUser);
        when(incomeRepository.findByUserIdAndYearAndMonth(1L, 2024, 3, pageable)).thenReturn(incomePage);

        Page<IncomeResponse> result = incomeService.getIncomesByUserAndMonth(1L, 2024, 3, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(userService).getUserEntityById(1L);
        verify(incomeRepository).findByUserIdAndYearAndMonth(1L, 2024, 3, pageable);
    }

    @Test
    @DisplayName("Обновить доход - успешно (только свой)")
    void updateIncome_Success() {
        IncomeRequest updateRequest = new IncomeRequest();
        updateRequest.setAmount(new BigDecimal("60000.00"));
        updateRequest.setSource("Премия");
        updateRequest.setCategory("Годовая премия");
        updateRequest.setDate(LocalDate.of(2024, 3, 20));

        when(incomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testIncome));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncomeResponse result = incomeService.updateIncome(1L, 1L, updateRequest);

        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("60000.00"));
        assertThat(result.getSource()).isEqualTo("Премия");
        assertThat(result.getCategory()).isEqualTo("Годовая премия");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 3, 20));

        verify(incomeRepository).findByIdAndUserId(1L, 1L);
        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    @DisplayName("Обновить доход - частичное обновление")
    void updateIncome_PartialUpdate_Success() {
        IncomeRequest partialRequest = new IncomeRequest();
        partialRequest.setAmount(new BigDecimal("55000.00"));

        when(incomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testIncome));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncomeResponse result = incomeService.updateIncome(1L, 1L, partialRequest);

        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("55000.00"));
        assertThat(result.getSource()).isEqualTo("Зарплата");
        assertThat(result.getCategory()).isEqualTo("Зарплата за март");

        verify(incomeRepository).findByIdAndUserId(1L, 1L);
        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    @DisplayName("Удалить доход - успешно (только свой)")
    void deleteIncome_Success() {
        when(incomeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testIncome));
        doNothing().when(incomeRepository).delete(any(Income.class));

        incomeService.deleteIncome(1L, 1L);

        verify(incomeRepository).findByIdAndUserId(1L, 1L);
        verify(incomeRepository).delete(testIncome);
    }

    @Test
    @DisplayName("Удалить доход - не найден (или не принадлежит пользователю)")
    void deleteIncome_NotFound_ThrowsException() {
        when(incomeRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incomeService.deleteIncome(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Income not found");

        verify(incomeRepository).findByIdAndUserId(99L, 1L);
        verify(incomeRepository, never()).delete(any(Income.class));
    }

    @Test
    @DisplayName("Добавить доход - проверка преобразования даты (пишем в БД полночь)")
    void addIncome_DateConversion_Success() {
        when(userService.getUserEntityById(1L)).thenReturn(testUser);

        ArgumentCaptor<Income> incomeCaptor = ArgumentCaptor.forClass(Income.class);
        when(incomeRepository.save(incomeCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IncomeResponse result = incomeService.addIncome(1L, incomeRequest);

        Income savedEntity = incomeCaptor.getValue();
        assertThat(savedEntity.getDate()).isNotNull();
        assertThat(savedEntity.getDate().toLocalDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(savedEntity.getDate().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);

        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 3, 15));

        verify(incomeRepository).save(any(Income.class));
    }
}
