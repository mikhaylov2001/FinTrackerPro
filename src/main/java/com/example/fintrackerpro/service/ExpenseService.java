package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.ExpenseResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserService userService;

    public ExpenseResponse addExpense(ExpenseRequest request) {
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
        return  ExpenseResponse.from(saved);
    }
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        return ExpenseResponse.from(expense);
    }
@Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByUser(Long userId, Pageable pageable) {
        userService.getUserById(userId);
        return expenseRepository.findByUserId(userId, pageable).map(ExpenseResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByUserAndMonth(Long userId, int year, int month, Pageable pageable) {
        log.info("Getting expenses for user {} {}/{}", userId, year, month);
        userService.getUserById(userId);
        return expenseRepository.findByUserIdAndYearAndMonth(userId, year, month, pageable).map(ExpenseResponse::from);
    }

    public ExpenseResponse updateExpense(Long expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));


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
        return ExpenseResponse.from(updated);
    }

    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + expenseId));
        expenseRepository.delete(expense);
        log.info("✅ Expense deleted: id={}", expenseId);
    }

}
