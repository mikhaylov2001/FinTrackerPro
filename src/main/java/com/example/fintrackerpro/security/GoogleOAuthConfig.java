package com.example.fintrackerpro.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class GoogleOAuthConfig {
    

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
