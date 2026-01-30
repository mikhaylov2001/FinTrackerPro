package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.MonthlySummaryDto;
import com.example.fintrackerpro.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Summary", description = "API для получения финансовой сводки и статистики")
@SecurityRequirement(name = "bearerAuth")
public class SummaryController {
    
    private final SummaryService summaryService;


    @Operation(
            summary = "Получить финансовую сводку за месяц",
            description = "Возвращает сводку доходов, расходов и баланса пользователя за указанный месяц"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Сводка получена успешно",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/{userId}/month/{year}/{month}")
    public ResponseEntity<MonthlySummaryDto> getMonthlySummary(
        @PathVariable Long userId,
        @PathVariable int year,
        @PathVariable int month
    ) {
        log.info("📊 GET /api/summary/{}/month/{}/{}", userId, year, month);
        MonthlySummaryDto summary = summaryService.getMonthlySummary(userId, year, month);
        return ResponseEntity.ok(summary);
    }
}
