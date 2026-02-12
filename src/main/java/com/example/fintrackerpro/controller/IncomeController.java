package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.IncomeResponse;
import com.example.fintrackerpro.entity.income.Income;
import com.example.fintrackerpro.entity.income.IncomeRequest;
import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.IncomeService;
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
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Incomes", description = "API для управления доходами пользователя")
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {

    private final IncomeService incomeService;

    @Operation(summary = "Создать новый доход (текущий пользователь)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Доход создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PostMapping
    public ResponseEntity<IncomeResponse> createIncome(@Valid @RequestBody IncomeRequest request,
                                                       Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("📥 POST /api/incomes (userId={})", userId);
        IncomeResponse body = incomeService.addIncome(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Получить доход по ID (текущий пользователь)")
    @GetMapping("/{incomeId}")
    public ResponseEntity<IncomeResponse> getIncome(@PathVariable Long incomeId, Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("📤 GET /api/incomes/{} (userId={})", incomeId, userId);
        return ResponseEntity.ok(incomeService.getIncomeById(userId, incomeId));
    }

    @Operation(summary = "Получить все доходы (текущий пользователь)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список доходов получен успешно",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Income.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/me")
    public ResponseEntity<Page<IncomeResponse>> getMyIncomes(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size,
                                                             Authentication auth) {
        Long userId = CurrentUser.id(auth);
        Pageable pageable = PageRequest.of(page, size);
        log.info("📤 GET /api/incomes/me (userId={})", userId);
        return ResponseEntity.ok(incomeService.getIncomesByUser(userId, pageable));
    }

    @Operation(summary = "Получить доходы за месяц (текущий пользователь)")
    @GetMapping("/me/month/{year}/{month}")
    public ResponseEntity<Page<IncomeResponse>> getMyIncomesByMonth(@PathVariable int year,
                                                                    @PathVariable int month,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    Authentication auth) {
        Long userId = CurrentUser.id(auth);
        Pageable pageable = PageRequest.of(page, size);
        log.info("📤 GET /api/incomes/me/month/{}/{} (userId={})", year, month, userId);
        return ResponseEntity.ok(incomeService.getIncomesByUserAndMonth(userId, year, month, pageable));
    }

    @Operation(summary = "Обновить доход (текущий пользователь)")
    @PutMapping("/{incomeId}")
    public ResponseEntity<IncomeResponse> updateIncome(@PathVariable Long incomeId,
                                                       @Valid @RequestBody IncomeRequest request,
                                                       Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("🔄 PUT /api/incomes/{} (userId={})", incomeId, userId);
        return ResponseEntity.ok(incomeService.updateIncome(userId, incomeId, request));
    }

    @Operation(summary = "Удалить доход (текущий пользователь)")
    @DeleteMapping("/{incomeId}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long incomeId, Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("🗑️ DELETE /api/incomes/{} (userId={})", incomeId, userId);
        incomeService.deleteIncome(userId, incomeId);
        return ResponseEntity.noContent().build();
    }

    // ---- Legacy: оставляем, но игнорируем userId из path
    @Deprecated
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<IncomeResponse>> getUserIncomesLegacy(@PathVariable Long userId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size,
                                                                     Authentication auth) {
        Long current = CurrentUser.id(auth);
        Pageable pageable = PageRequest.of(page, size);
        log.warn("Legacy incomes list used: path userId={}, current userId={}", userId, current);
        return ResponseEntity.ok(incomeService.getIncomesByUser(current, pageable));
    }

    @Deprecated
    @GetMapping("/user/{userId}/month/{year}/{month}")
    public ResponseEntity<Page<IncomeResponse>> getUserIncomesByMonthLegacy(@PathVariable Long userId,
                                                                            @PathVariable int year,
                                                                            @PathVariable int month,
                                                                            @RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            Authentication auth) {
        Long current = CurrentUser.id(auth);
        Pageable pageable = PageRequest.of(page, size);
        log.warn("Legacy incomes by month used: path userId={}, current userId={}", userId, current);
        return ResponseEntity.ok(incomeService.getIncomesByUserAndMonth(current, year, month, pageable));
    }
}

