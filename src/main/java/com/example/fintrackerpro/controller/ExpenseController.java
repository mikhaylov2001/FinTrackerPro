package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.ExpenseResponse;
import com.example.fintrackerpro.entity.expense.Expense;
import com.example.fintrackerpro.entity.expense.ExpenseRequest;
import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Expenses", description = "API для управления расходами пользователя")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Operation(summary = "Создать новый расход (текущий пользователь)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Расход успешно создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request,
                                                         Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("📥 POST /api/expenses (userId={})", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.addExpense(userId, request));
    }

    @Operation(summary = "Получить расход по ID (текущий пользователь)")
    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable Long expenseId, Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("📤 GET /api/expenses/{} (userId={})", expenseId, userId);
        return ResponseEntity.ok(expenseService.getExpenseById(userId, expenseId));
    }

    @Operation(summary = "Получить все расходы (текущий пользователь)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список расходов получен успешно",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Expense.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/me")
    public ResponseEntity<Page<ExpenseResponse>> getMyExpenses(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               Authentication auth) {
        Long userId = CurrentUser.id(auth);
        Pageable pageable = PageRequest.of(page, size);
        log.info("📤 GET /api/expenses/me (userId={})", userId);
        return ResponseEntity.ok(expenseService.getExpensesByUser(userId, pageable));
    }

    @Operation(summary = "Получить расходы за месяц (текущий пользователь)")
    @GetMapping("/me/month/{year}/{month}")
    public ResponseEntity<Page<ExpenseResponse>> getMyExpensesByMonth(@PathVariable int year,
                                                                      @PathVariable int month,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size,
                                                                      Authentication auth) {
        Long userId = CurrentUser.id(auth);
        Pageable pageable = PageRequest.of(page, size);
        log.info("📤 GET /api/expenses/me/month/{}/{} (userId={})", year, month, userId);
        return ResponseEntity.ok(expenseService.getExpensesByUserAndMonth(userId, year, month, pageable));
    }

    @Operation(summary = "Обновить расход (текущий пользователь)")
    @PutMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long expenseId,
                                                         @Valid @RequestBody ExpenseRequest request,
                                                         Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("🔄 PUT /api/expenses/{} (userId={})", expenseId, userId);
        return ResponseEntity.ok(expenseService.updateExpense(userId, expenseId, request));
    }

    @Operation(summary = "Удалить расход (текущий пользователь)")
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId, Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("🗑️ DELETE /api/expenses/{} (userId={})", expenseId, userId);
        expenseService.deleteExpense(userId, expenseId);
        return ResponseEntity.noContent().build();
    }

    // ---- Legacy: оставляем маршруты, но игнорируем userId из path
    @Deprecated
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ExpenseResponse>> getUserExpensesLegacy(@PathVariable Long userId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       Authentication auth) {
        Long current = CurrentUser.id(auth);
        Pageable pageable = PageRequest.of(page, size);
        log.warn("Legacy expenses list used: path userId={}, current userId={}", userId, current);
        return ResponseEntity.ok(expenseService.getExpensesByUser(current, pageable));
    }

    @Deprecated
    @GetMapping("/user/{userId}/month/{year}/{month}")
    public ResponseEntity<Page<ExpenseResponse>> getUserExpensesByMonthLegacy(@PathVariable Long userId,
                                                                              @PathVariable int year,
                                                                              @PathVariable int month,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size,
                                                                              Authentication auth) {
        Long current = CurrentUser.id(auth);
        Pageable pageable = PageRequest.of(page, size);
        log.warn("Legacy expenses by month used: path userId={}, current userId={}", userId, current);
        return ResponseEntity.ok(expenseService.getExpensesByUserAndMonth(current, year, month, pageable));
    }
}

