package com.example.fintrackerpro.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.json.JsonMapper.builder;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 1. Проверяем валидность и срок действия
                if (jwtUtil.isExpired(token)) {
                    unauthorized(response, "Token has expired");
                    return;
                }

                // 2. Извлекаем ID пользователя
                Long userId = jwtUtil.extractUserId(token);

                if (userId != null) {
                    // 3. Создаем объект аутентификации.
                    // Добавляем ROLE_USER, чтобы Spring Security пропускал запросы в .authenticated()
                    var auth = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 4. Устанавливаем в контекст
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("User {} authenticated via JWT", userId);
                }

            } catch (JwtException e) {
                log.warn("JWT verification failed: {}", e.getMessage());
                unauthorized(response, "Invalid token");
                return;
            } catch (Exception e) {
                log.error("Unexpected error in JWT filter", e);
                unauthorized(response, "Authentication error");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void unauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Используем ObjectMapper для формирования чистого JSON
        var mapper = builder().build();
        String json = mapper.writeValueAsString(Map.of(
                "error", msg,
                "status", 401,
                "timestamp", System.currentTimeMillis()
        ));

        response.getWriter().write(json);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Пропускаем все OPTIONS запросы (CORS Preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // 2. КРИТИЧЕСКОЕ: Пропускаем все, что связано с авторизацией
        // Добавь проверку на /api/auth без жесткой привязки к слешу в конце
        if (path.startsWith("/api/auth")) return true;

        // 3. Пропускаем Swagger
        if (path.contains("/swagger-ui") || path.contains("/v3/api-docs")) return true;

        return false;
    }
}