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

        String path = request.getRequestURI();
        String token = extractToken(request);

        // Если это путь авторизации, просто идем дальше, не трогая токены вообще
        if (path.contains("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long userId = jwtUtil.extractUserId(token);
                if (userId != null && !jwtUtil.isExpired(token)) {
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // Если токен битый (например, после смены секрета),
                // мы НЕ кидаем 403 сразу, а просто идем дальше.
                // SecurityConfig сам заблокирует доступ к защищенным путям.
                log.error("JWT validation failed: {}", e.getMessage());
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
        return path.startsWith("/api/auth/"); // Пропускаем всё, что связано с авторизацией
    }
}