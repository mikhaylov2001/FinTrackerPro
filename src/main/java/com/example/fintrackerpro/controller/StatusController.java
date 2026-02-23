package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.repository.UserRepository;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatusController {

  private final UserRepository userRepository;
  private final HealthEndpoint healthEndpoint;
  private final InfoEndpoint infoEndpoint;

  public StatusController(UserRepository userRepository,
                          HealthEndpoint healthEndpoint,
                          InfoEndpoint infoEndpoint) {
    this.userRepository = userRepository;
    this.healthEndpoint = healthEndpoint;
    this.infoEndpoint = infoEndpoint;
  }

  @GetMapping("/")
  public Map<String, Object> root() {
    long users = userRepository.count();

    var health = healthEndpoint.health();
    var info = infoEndpoint.info();

    return Map.of(
      "message", "FinTrackerPro API is running",
      "status", health.getStatus().getCode(),
      "usersTotal", users,
      "info", info,
      "links", Map.of(
        "health", "/actuator/health",
        "prometheus", "/actuator/prometheus",
        "swagger", "/swagger-ui.html"
      )
    );
  }
}
