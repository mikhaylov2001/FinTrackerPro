package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.ExpenseResponse;
import com.example.fintrackerpro.entity.expense.Expense;
import com.example.fintrackerpro.entity.expense.ExpenseRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

        req = new ExpenseRequest();
        req.setUserId(1L);
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
    void addExpense_Success() {
        when(userService.getUserById(1L)).thenReturn(user);
        when(expenseRepository.save(any(Expense.class))).thenReturn(entity);

        ExpenseResponse dto = expenseService.addExpense(req);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 3, 15));

        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void getExpenseById_Success() {
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(entity));

        ExpenseResponse dto = expenseService.getExpenseById(10L);

        assertThat(dto.getId()).isEqualTo(10L);
    }

    @Test
    void updateExpense_Success() {
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseRequest upd = new ExpenseRequest();
        upd.setUserId(1L);
        upd.setAmount(new BigDecimal("2000.00"));
        upd.setCategory("Продукты");
        upd.setDescription("Апдейт");
        upd.setDate(LocalDate.of(2024, 3, 20));

        ExpenseResponse dto = expenseService.updateExpense(10L, upd);

        assertThat(dto.getAmount()).isEqualByComparingTo("2000.00");
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 3, 20));
    }
}