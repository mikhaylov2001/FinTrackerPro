package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatusController {

    private final UserRepository userRepository;
    private final HealthEndpoint healthEndpoint;
    private final InfoEndpoint infoEndpoint;

    @GetMapping("/status")
    public Map<String, Object> status() {
        long users = userRepository.count();
        var health = healthEndpoint.health();
        var info = infoEndpoint.info();

        // Пытаемся достать версию из info, если она там есть
        Object version = null;
        if (info != null) {
            Object build = info.get("build");
            if (build instanceof Map<?, ?> buildMap) {
                version = buildMap.get("version");
            }
        }

        String serverTime = OffsetDateTime.now(ZoneOffset.UTC).toString();

        return Map.of(
                "message", "FinTrackerPro API is running",
                "status", health.getStatus().getCode(),
                "usersTotal", users,
                "version", version,          // может быть null, если нет в /info
                "serverTime", serverTime,    // ISO-8601, UTC
                "info", info,
                "links", Map.of(
                        "health", "/actuator/health",
                        "prometheus", "/actuator/prometheus",
                        "swagger", "/swagger-ui.html"
                )
        );
    }
}
