package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.DataService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data", description = "Управление пользовательскими данными")
@SecurityRequirement(name = "bearerAuth")
public class DataController {

    private final DataService dataService;

    /**
     * DELETE /api/data/me/month/{year}/{month}?type=income|expenses|all
     */
    @DeleteMapping("/me/month/{year}/{month}")
    public ResponseEntity<?> deleteMonthData(
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam(defaultValue = "all") String type,
            Authentication auth
    ) {
        Long userId = CurrentUser.id(auth);
        log.info("🗑 DELETE /api/data/me/month/{}/{} type={} userId={}", year, month, type, userId);

        try {
            dataService.deleteMonthData(userId, year, month, type);
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "year", year,
                "month", month,
                "type", type
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
