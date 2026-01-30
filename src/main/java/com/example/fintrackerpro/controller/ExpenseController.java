package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.entity.expense.Expense;
import com.example.fintrackerpro.entity.expense.ExpenseRequest;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Expenses", description = "API для управления расходами пользователя")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {
    
    private final ExpenseService expenseService;

    @Operation(
            summary = "Создать новый расход",
            description = "Добавляет новый расход для текущего пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Расход успешно создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })

    @PostMapping
    public ResponseEntity<Expense> createExpense(@Valid @RequestBody ExpenseRequest request) {
        log.info("📥 POST /api/expenses - Create expense for user {}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.addExpense(request));
    }
    @Operation(
            summary = "Получить расход по ID",
            description = "Возвращает информацию о конкретном расходе по его идентификатору"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Расход найден"),
            @ApiResponse(responseCode = "404", description = "Расход не найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })



    @GetMapping("/{expenseId}")
    public ResponseEntity<Expense> getExpense(@PathVariable Long expenseId) {
        log.info("📤 GET /api/expenses/{}", expenseId);
        return ResponseEntity.ok(expenseService.getExpenseById(expenseId));
    }

    @Operation(
            summary = "Получить все расходы пользователя",
            description = "Возвращает список всех расходов текущего аутентифицированного пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список расходов получен успешно",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Expense.class))
            ),
            @ApiResponse(responseCode = "401", description = "Не авторизован (отсутствует или некорректный токен)")
    })

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Expense>> getUserExpenses(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📤 GET /api/expenses/user/{}", userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Expense> expensesByUser = expenseService.getExpensesByUser(userId, pageable);
        return ResponseEntity.ok(expensesByUser);
    }
    @Operation(
            summary = "Получить расходы по категории и периоду",
            description = "Фильтрует расходы пользователя по месяцу, году и опциональной категории"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список расходов получен"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })


    @GetMapping("/user/{userId}/month/{year}/{month}")
    public ResponseEntity<Page<Expense>> getUserExpensesByMonth(
        @PathVariable Long userId,
        @PathVariable int year,
        @PathVariable int month,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📤 GET /api/expenses/user/{}/month/{}/{}", userId, year, month);
        Pageable pageable = PageRequest.of(page, size);
        Page<Expense> expensesByUserAndMonth = expenseService.getExpensesByUserAndMonth(userId, year, month, pageable);
        return ResponseEntity.ok(expensesByUserAndMonth);
    }
    @Operation(
            summary = "Обновить расход",
            description = "Обновляет существующий расход пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Расход успешно обновлён"),
            @ApiResponse(responseCode = "404", description = "Расход не найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })



    @PutMapping("/{expenseId}")
    public ResponseEntity<Expense> updateExpense(
        @PathVariable Long expenseId,
        @Valid @RequestBody ExpenseRequest request
    ) {
        log.info("🔄 PUT /api/expenses/{}", expenseId);
        Expense body = expenseService.updateExpense(expenseId, request);
        return ResponseEntity.ok(body);
    }
    @Operation(
            summary = "Удалить расход",
            description = "Удаляет расход пользователя по ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Расход успешно удалён"),
            @ApiResponse(responseCode = "404", description = "Расход не найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })



    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        log.info("🗑️  DELETE /api/expenses/{}", expenseId);
        expenseService.deleteExpense(expenseId);
        return ResponseEntity.noContent().build();
    }
}
