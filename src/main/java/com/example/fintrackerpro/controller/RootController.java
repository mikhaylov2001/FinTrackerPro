package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class RootController {

    private static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UserRepository userRepository;
    private final HealthEndpoint healthEndpoint;
    private final InfoEndpoint infoEndpoint;

    @GetMapping("/")
    public String index(Model model) {
        long usersTotal = userRepository.count();
        var health = healthEndpoint.health();
        var info = infoEndpoint.info();

        String status = health.getStatus().getCode();

        String version = null;

        if (info instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> infoMap = (Map<String, Object>) info;

            Object app = infoMap.get("app");
            if (app instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> appMap = (Map<String, Object>) app;
                Object v = appMap.get("version");
                if (v != null) {
                    version = v.toString();
                }
            }
        }



        ZonedDateTime now = ZonedDateTime.now(MOSCOW_ZONE);
        String serverTime = now.format(TIME_FMT);  // 15:31:10
        String serverDate = now.format(DATE_FMT);  // 23.02.2026

        model.addAttribute("status", status);
        model.addAttribute("usersTotal", usersTotal);
        model.addAttribute("info", info);
        model.addAttribute("version", version);
        model.addAttribute("serverTime", serverTime);
        model.addAttribute("serverDate", serverDate);
        model.addAttribute("serverZone", MOSCOW_ZONE.toString());

        return "index";
    }
}
