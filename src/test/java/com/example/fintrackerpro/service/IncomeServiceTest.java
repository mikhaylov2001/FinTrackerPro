package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.income.Income;
import com.example.fintrackerpro.entity.income.IncomeRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.repository.IncomeRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

        incomeRequest = new IncomeRequest();
        incomeRequest.setUserId(1L);
        incomeRequest.setAmount(new BigDecimal("50000.00"));
        incomeRequest.setSource("Зарплата");
        incomeRequest.setCategory("Зарплата за март");
        incomeRequest.setDate(LocalDate.of(2024, 3, 15));
    }

    @Test
    @DisplayName("Добавить доход - успешно")
    void addIncome_Success() {
        // Given
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(incomeRepository.save(any(Income.class))).thenReturn(testIncome);

        // When
        Income result = incomeService.addIncome(incomeRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(result.getSource()).isEqualTo("Зарплата");
        assertThat(result.getUser()).isEqualTo(testUser);

        verify(userService).getUserById(1L);
        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    @DisplayName("Добавить доход - пользователь не найден")
    void addIncome_UserNotFound_ThrowsException() {
        // Given
        when(userService.getUserById(1L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // When & Then
        assertThatThrownBy(() -> incomeService.addIncome(incomeRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userService).getUserById(1L);
        verify(incomeRepository, never()).save(any(Income.class));
    }

    @Test
    @DisplayName("Получить доход по ID - успешно")
    void getIncomeById_Success() {
        // Given
        when(incomeRepository.findById(1L)).thenReturn(Optional.of(testIncome));

        // When
        Income result = incomeService.getIncomeById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));

        verify(incomeRepository).findById(1L);
    }

    @Test
    @DisplayName("Получить доход по ID - не найден")
    void getIncomeById_NotFound_ThrowsException() {
        // Given
        when(incomeRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> incomeService.getIncomeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Income not found");

        verify(incomeRepository).findById(99L);
    }

    @Test
    @DisplayName("Получить доходы пользователя - успешно (с Pageable)")
    void getIncomesByUser_WithPageable_Success() {
        // Given
        Page<Income> incomePage = new PageImpl<>(Arrays.asList(testIncome));
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.getUserById(1L)).thenReturn(testUser);
        // ✅ ИСПРАВЛЕНО: правильное название метода
        when(incomeRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(incomePage);

        // When
        Page<Income> result = incomeService.getIncomesByUser(1L, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testIncome);

        verify(userService).getUserById(1L);
        // ✅ ИСПРАВЛЕНО: проверка правильного метода
        verify(incomeRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
    }


    @Test
    @DisplayName("Получить доходы за месяц - успешно")
    void getIncomesByUserAndMonth_Success() {
        // Given
        Page<Income> incomePage = new PageImpl<>(Arrays.asList(testIncome));
        Pageable pageable = PageRequest.of(0, 10);

        when(userService.getUserById(1L)).thenReturn(testUser);
        when(incomeRepository.findByUserIdAndYearAndMonth(1L, 2024, 3, pageable))
                .thenReturn(incomePage);

        // When
        Page<Income> result = incomeService.getIncomesByUserAndMonth(1L, 2024, 3, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(userService).getUserById(1L);
        verify(incomeRepository).findByUserIdAndYearAndMonth(1L, 2024, 3, pageable);
    }

    @Test
    @DisplayName("Обновить доход - успешно")
    void updateIncome_Success() {
        // Given
        IncomeRequest updateRequest = new IncomeRequest();
        updateRequest.setAmount(new BigDecimal("60000.00"));
        updateRequest.setSource("Премия");
        updateRequest.setCategory("Годовая премия");
        updateRequest.setDate(LocalDate.of(2024, 3, 20));

        when(incomeRepository.findById(1L)).thenReturn(Optional.of(testIncome));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> {
            Income saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        Income result = incomeService.updateIncome(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("60000.00"));
        assertThat(result.getSource()).isEqualTo("Премия");
        assertThat(result.getCategory()).isEqualTo("Годовая премия");

        verify(incomeRepository).findById(1L);
        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    @DisplayName("Обновить доход - частичное обновление")
    void updateIncome_PartialUpdate_Success() {
        // Given
        IncomeRequest partialRequest = new IncomeRequest();
        partialRequest.setAmount(new BigDecimal("55000.00"));
        // Остальные поля null - не обновляются

        when(incomeRepository.findById(1L)).thenReturn(Optional.of(testIncome));
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Income result = incomeService.updateIncome(1L, partialRequest);

        // Then
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("55000.00"));
        // Старые значения сохранились
        assertThat(result.getSource()).isEqualTo("Зарплата");
        assertThat(result.getCategory()).isEqualTo("Зарплата за март");

        verify(incomeRepository).save(any(Income.class));
    }

    @Test
    @DisplayName("Удалить доход - успешно")
    void deleteIncome_Success() {
        // Given
        when(incomeRepository.findById(1L)).thenReturn(Optional.of(testIncome));
        doNothing().when(incomeRepository).delete(any(Income.class));

        // When
        incomeService.deleteIncome(1L);

        // Then
        verify(incomeRepository).findById(1L);
        verify(incomeRepository).delete(testIncome);
    }

    @Test
    @DisplayName("Удалить доход - не найден")
    void deleteIncome_NotFound_ThrowsException() {
        // Given
        when(incomeRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> incomeService.deleteIncome(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Income not found");

        verify(incomeRepository).findById(99L);
        verify(incomeRepository, never()).delete(any(Income.class));
    }

    @Test
    @DisplayName("Добавить доход - проверка преобразования даты")
    void addIncome_DateConversion_Success() {
        // Given
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(incomeRepository.save(any(Income.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Income result = incomeService.addIncome(incomeRequest);

        // Then
        assertThat(result.getDate()).isNotNull();
        assertThat(result.getDate().toLocalDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(result.getDate().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);

        verify(incomeRepository).save(any(Income.class));
    }
}
