package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.IncomeResponse;
import com.example.fintrackerpro.entity.income.Income;
import com.example.fintrackerpro.entity.income.IncomeRequest;
import com.example.fintrackerpro.service.IncomeService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Income", description = "API для управления доходами пользователя")
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {
    
    private final IncomeService incomeService;


    @Operation(summary = "Создать новый доход")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Доход создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные")
    })

    @PostMapping
    public ResponseEntity<IncomeResponse> createIncome(@Valid @RequestBody IncomeRequest request) {
        log.info("📥 POST /api/incomes - Create income for user {}", request.getUserId());
        IncomeResponse body = incomeService.addIncome(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }


    @Operation(summary = "Получить доход по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Доход найден"),
            @ApiResponse(responseCode = "404", description = "Доход не найден")
    })

    @GetMapping("/{incomeId}")
    public ResponseEntity<IncomeResponse> getIncome(@PathVariable Long incomeId) {
        log.info("📤 GET /api/incomes/{}", incomeId);
        IncomeResponse incomeById = incomeService.getIncomeById(incomeId);
        return ResponseEntity.ok(incomeById);
    }
    @Operation(
            summary = "Получить все доходы пользователя",
            description = "Возвращает список всех доходов текущего аутентифицированного пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список доходов получен успешно"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })


    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<IncomeResponse>> getUserIncomes(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📤 GET /api/incomes/user/{}", userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<IncomeResponse> incomesByUser = incomeService.getIncomesByUser(userId, pageable);
        return ResponseEntity.ok(incomesByUser);
    }



    @Operation(summary = "Получение доходов по месяцам")
    @GetMapping("/user/{userId}/month/{year}/{month}")
    public ResponseEntity<Page<IncomeResponse>> getUserIncomesByMonth(
        @PathVariable Long userId,
        @PathVariable int year,
        @PathVariable int month,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📤 GET /api/incomes/user/{}/month/{}/{}", userId, year, month);
        Pageable pageable = PageRequest.of(page, size);
        Page<IncomeResponse> incomesByUserAndMonth = incomeService.getIncomesByUserAndMonth(userId, year, month, pageable);
        return ResponseEntity.ok(incomesByUserAndMonth);
    }
    @Operation(summary = "Обновить доход")
    @PutMapping("/{incomeId}")
    public ResponseEntity<IncomeResponse> updateIncome(
        @PathVariable Long incomeId,
        @Valid @RequestBody IncomeRequest request
    ) {
        log.info("🔄 PUT /api/incomes/{}", incomeId);
        IncomeResponse body = incomeService.updateIncome(incomeId, request);
        return ResponseEntity.ok(body);
    }
    @Operation(summary = "Удалить доход")
    @DeleteMapping("/{incomeId}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long incomeId) {
        log.info("🗑️  DELETE /api/incomes/{}", incomeId);
        incomeService.deleteIncome(incomeId);
        return ResponseEntity.noContent().build();
    }
}
