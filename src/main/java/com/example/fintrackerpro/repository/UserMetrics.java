package com.example.fintrackerpro.repository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class UserMetrics {

  public UserMetrics(MeterRegistry registry, UserRepository userRepository) {
    registry.gauge("fintracker_users_total",
            Tags.of("service", "fintracker-api", "env", "prod"),
            userRepository,
            repo -> repo.count());

  }
}
