package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.expense.Expense;
import com.example.fintrackerpro.entity.expense.ExpenseRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;

    public Expense addExpense(ExpenseRequest request) {
        User user = userService.getUserById(request.getUserId());

        Expense expense = Expense.builder()
                .user(user)
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(request.getDate().atTime(LocalTime.MIDNIGHT))
                .build();

        Expense saved = expenseRepository.save(expense);
        log.info("✅ Expense created: id={}, userId={}, amount={}, category={}",
                saved.getId(), user.getId(), saved.getAmount(), saved.getCategory());
        return saved;
    }

    public Expense getExpenseById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> {
                    log.error("❌ Expense not found with id={}", expenseId);
                    return new ResourceNotFoundException("Expense not found with id: " + expenseId);
                });
    }

    public Page<Expense> getExpensesByUser(Long userId, Pageable pageable) {
        userService.getUserById(userId);
        return expenseRepository.findByUserId(userId, pageable);
    }

    public Page<Expense> getExpensesByUserAndMonth(Long userId, int year, int month, Pageable pageable) {
        log.info("Getting expenses for user {} {}/{}", userId, year, month);
        userService.getUserById(userId);
        return expenseRepository.findByUserIdAndYearAndMonth(userId, year, month, pageable);
    }

    public Expense updateExpense(Long expenseId, ExpenseRequest request) {
        Expense expense = getExpenseById(expenseId);

        if (request.getAmount() != null) {
            expense.setAmount(request.getAmount());
        }
        if (request.getCategory() != null) {
            expense.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getDate() != null) {
            expense.setDate(request.getDate().atTime(LocalTime.MIDNIGHT));
        }

        Expense updated = expenseRepository.save(expense);
        log.info("✅ Expense updated: id={}, amount={}", expenseId, updated.getAmount());
        return updated;
    }

    public void deleteExpense(Long expenseId) {
        Expense expense = getExpenseById(expenseId);
        expenseRepository.delete(expense);
        log.info("✅ Expense deleted: id={}", expenseId);
    }

}
