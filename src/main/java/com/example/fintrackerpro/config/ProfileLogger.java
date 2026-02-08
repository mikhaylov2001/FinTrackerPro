package com.example.fintrackerpro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileLogger {

    private final Environment env;

    @EventListener(ApplicationReadyEvent.class)
    public void logProfiles() {
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
        log.info("Default profiles: {}", Arrays.toString(env.getDefaultProfiles()));
    }
}
