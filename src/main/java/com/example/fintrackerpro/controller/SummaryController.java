package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.MonthlySummaryDto;
import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Summary", description = "API для получения финансовой сводки и статистики")
@SecurityRequirement(name = "bearerAuth")
public class SummaryController {

    private final SummaryService summaryService;

    @Operation(summary = "Получить финансовую сводку за месяц (текущий пользователь)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Сводка получена успешно"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/me/month/{year}/{month}")
    public ResponseEntity<MonthlySummaryDto> getMyMonthlySummary(
            @PathVariable int year,
            @PathVariable int month,
            Authentication auth
    ) {
        Long userId = CurrentUser.id(auth);
        log.info("📊 GET /api/summary/me/month/{}/{} (userId={})", year, month, userId);
        return ResponseEntity.ok(summaryService.getMonthlySummary(userId, year, month));
    }

    @Operation(summary = "Получить список использованных месяцев (текущий пользователь)")
    @GetMapping("/me/months")
    public ResponseEntity<List<String>> getMyUsedMonths(Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("📅 GET /api/summary/me/months (userId={})", userId);
        return ResponseEntity.ok(summaryService.getUsedMonths(userId));
    }

    @Operation(summary = "Получить итоги за все месяцы (текущий пользователь)")
    @GetMapping("/me/monthly/all")
    public ResponseEntity<List<MonthlySummaryDto>> getMyAllMonthlySummaries(Authentication auth) {
        Long userId = CurrentUser.id(auth);
        log.info("📊 GET /api/summary/me/monthly/all (userId={})", userId);
        return ResponseEntity.ok(summaryService.getAllMonthlySummaries(userId));
    }

    // ---- Legacy (чтобы старый фронт не упал). Игнорируем userId из path.
    @Deprecated
    @GetMapping("/{userId}/month/{year}/{month}")
    public ResponseEntity<MonthlySummaryDto> getMonthlySummaryLegacy(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month,
            Authentication auth
    ) {
        Long current = CurrentUser.id(auth);
        if (!current.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.warn("Legacy summary used: path userId={}, current userId={}", userId, current);
        return ResponseEntity.ok(summaryService.getMonthlySummary(current, year, month));
    }

    @Deprecated
    @GetMapping("/{userId}/months")
    public ResponseEntity<List<String>> getUsedMonthsLegacy(@PathVariable Long userId, Authentication auth) {
        Long current = CurrentUser.id(auth);
        if (!current.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.warn("Legacy months used: path userId={}, current userId={}", userId, current);
        return ResponseEntity.ok(summaryService.getUsedMonths(current));
    }

    @Deprecated
    @GetMapping("/{userId}/monthly/all")
    public ResponseEntity<List<MonthlySummaryDto>> getAllMonthlySummariesLegacy(@PathVariable Long userId, Authentication auth) {
        Long current = CurrentUser.id(auth);
        if (!current.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.warn("Legacy monthly/all used: path userId={}, current userId={}", userId, current);
        return ResponseEntity.ok(summaryService.getAllMonthlySummaries(current));
    }
}
