package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class RootController {

  private final UserRepository userRepository;
  private final HealthEndpoint healthEndpoint;
  private final InfoEndpoint infoEndpoint;

  @GetMapping("/")
  public String index(Model model) {
    long usersTotal = userRepository.count();

    var health = healthEndpoint.health();
    var info = infoEndpoint.info();

    model.addAttribute("status", health.getStatus().getCode());
    model.addAttribute("usersTotal", usersTotal);
    model.addAttribute("info", info);

    return "index"; // templates/index.html
  }
}
