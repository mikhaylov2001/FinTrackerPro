package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatusController {

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UserRepository userRepository;
    private final HealthEndpoint healthEndpoint;
    private final InfoEndpoint infoEndpoint;

    @GetMapping("/status")
    public Map<String, Object> status() {
        long users = userRepository.count();
        var health = healthEndpoint.health();
        var info = infoSafe();

        String rawStatus = health.getStatus().getCode();

        // Версия из info.app.version (если есть)
        String version = null;
        if (info != null) {
            Object app = info.get("app");
            if (app instanceof Map<?, ?> appMap) {
                Object v = appMap.get("version");
                if (v != null) {
                    version = v.toString();
                }
            }
        }

        var now = ZonedDateTime.now(MOSCOW_ZONE);
        String time = now.format(TIME_FMT);       // 15:32:10
        String date = now.format(DATE_FMT);       // 23.02.2026

        return Map.of(
                "message", "FinTrackerPro API is running",
                "status", rawStatus,
                "statusText", "UP".equalsIgnoreCase(rawStatus) ? "Работает" : "Проблемы",
                "usersTotal", users,
                "version", version,
                "time", time,
                "date", date,
                "zone", MOSCOW_ZONE.toString(),
                "info", info
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> infoSafe() {
        Object i = infoEndpoint.info();
        if (i instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }
}
