package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

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

    String status = health.getStatus().getCode();

    // Пытаемся достать версию из info.app.version (если прописана в application.yml)
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

    String serverTime = OffsetDateTime.now(ZoneOffset.UTC).toString();

    model.addAttribute("status", status);
    model.addAttribute("usersTotal", usersTotal);
    model.addAttribute("info", info);
    model.addAttribute("version", version);
    model.addAttribute("serverTime", serverTime);

    return "index";
  }
}
