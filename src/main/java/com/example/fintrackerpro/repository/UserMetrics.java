package com.example.fintrackerpro.repository;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class UserMetrics {

  public UserMetrics(MeterRegistry registry, UserRepository userRepository) {
    registry.gauge("fintracker_users_total", userRepository,
        repo -> repo.count());
  }
}
