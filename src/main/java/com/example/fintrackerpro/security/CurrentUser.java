package com.example.fintrackerpro.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class CurrentUser {

    private CurrentUser() {}

    public static Long id(Authentication auth) {
        auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new IllegalStateException("Unauthenticated");

        Object principal = auth.getPrincipal();

        if (principal instanceof Long l) return l;

        if (principal instanceof Integer i) return i.longValue();

        if (principal instanceof String s && !s.isBlank()) return Long.parseLong(s);

        if (principal instanceof UserDetails ud && ud.getUsername() != null) {
            return Long.parseLong(ud.getUsername());
        }

        // Последний шанс: getName() (часто это username)
        String name = auth.getName();
        if (name != null && !name.isBlank()) return Long.parseLong(name);

        throw new IllegalStateException("Unauthenticated");
    }
}
