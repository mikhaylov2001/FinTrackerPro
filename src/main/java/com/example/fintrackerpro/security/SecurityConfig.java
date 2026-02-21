package com.example.fintrackerpro.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

import static org.springframework.http.HttpMethod.GET;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Важно: при allowCredentials=true нельзя "*", только явные origin'ы
        // Добавь сюда ВСЕ домены, с которых реально ходит фронт (prod + preview).
        cfg.setAllowedOrigins(List.of(
                "https://fintrackerpro.vercel.app"
                // пример: "https://fintrackerpro-git-main-xxxxx.vercel.app"
        ));

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Разрешаем нужные заголовки для JWT + обычных запросов
        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With"
        ));

        // Если хочешь читать Authorization из ответа (обычно не нужно, т.к. ты отдаёшь token в JSON)
        cfg.setExposedHeaders(List.of("Authorization"));

        // Нужно, чтобы браузер мог отправлять refresh cookies
        cfg.setAllowCredentials(true);

        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    public SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(GET, "/", "/index.html", "/favicon.ico").permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
