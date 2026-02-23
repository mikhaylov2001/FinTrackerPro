package com.example.fintrackerpro.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

  private final Counter registrationCounter;
  private final Counter loginSuccessCounter;
  private final Counter loginFailureCounter;
  private final Counter businessErrorCounter;

  public MetricsService(MeterRegistry registry) {
    this.registrationCounter =
            registry.counter("fintracker_user_registrations_total",
                    "service", "fintracker-api", "env", "prod");

    this.loginSuccessCounter =
            registry.counter("fintracker_logins_total",
                    "status", "success", "service", "fintracker-api", "env", "prod");

    this.loginFailureCounter =
            registry.counter("fintracker_logins_total",
                    "status", "failure", "service", "fintracker-api", "env", "prod");

    this.businessErrorCounter =
            registry.counter("fintracker_errors_total",
                    "type", "business", "service", "fintracker-api", "env", "prod");
  }


  public void incRegistration() { registrationCounter.increment(); }

  public void incLoginSuccess() { loginSuccessCounter.increment(); }

  public void incLoginFailure() { loginFailureCounter.increment(); }

  public void incBusinessError() { businessErrorCounter.increment(); }


}


