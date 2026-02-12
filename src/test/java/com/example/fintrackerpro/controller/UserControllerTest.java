package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserDto;
import com.example.fintrackerpro.security.JwtAuthenticationFilter;
import com.example.fintrackerpro.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc
@DisplayName("UserController WebMvc Tests")
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;

    private static RequestPostProcessor auth(long userId) {
        Authentication a = new UsernamePasswordAuthenticationToken(
                userId, "N/A", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return authentication(a);
    }

    @Test
    @DisplayName("GET /api/users/me - ok")
    void getMe_ok() throws Exception {
        when(userService.getUserById(1L))
                .thenReturn(new UserDto(1L, "testuser", "test@example.com"));

        mockMvc.perform(get("/api/users/me").with(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userService).getUserById(1L);
    }

    @Test
    @DisplayName("GET /api/users/{userId} - ok (только свой id)")
    void getUserById_ok_onlySelf() throws Exception {
        when(userService.getUserById(1L))
                .thenReturn(new UserDto(1L, "testuser", "test@example.com"));

        mockMvc.perform(get("/api/users/1").with(auth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userService).getUserById(1L);
    }

    @Test
    @DisplayName("GET /api/users/{userId} - forbidden (чужой id)")
    void getUserById_forbidden_otherUser() throws Exception {
        mockMvc.perform(get("/api/users/999").with(auth(1L)))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException ex = (ResponseStatusException) result.getResolvedException();
                    assertThat(ex.getStatusCode().value()).isEqualTo(403);
                });

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("PUT /api/users/{userId} - ok (только свой id)")
    void updateUser_ok_onlySelf() throws Exception {
        User req = new User();
        req.setEmail("new@example.com");

        when(userService.updateUser(eq(1L), any(User.class)))
                .thenReturn(new UserDto(1L, "testuser", "new@example.com"));

        mockMvc.perform(put("/api/users/1")
                        .with(auth(1L))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));

        verify(userService).updateUser(eq(1L), any(User.class));
    }

    @Test
    @DisplayName("PUT /api/users/{userId} - forbidden (чужой id)")
    void updateUser_forbidden_otherUser() throws Exception {
        User req = new User();
        req.setEmail("hack@example.com");

        mockMvc.perform(put("/api/users/999")
                        .with(auth(1L))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException ex = (ResponseStatusException) result.getResolvedException();
                    assertThat(ex.getStatusCode().value()).isEqualTo(403);
                });

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - noContent (только свой id)")
    void deleteUser_ok_onlySelf() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1")
                        .with(auth(1L))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    @DisplayName("DELETE /api/users/{userId} - forbidden (чужой id)")
    void deleteUser_forbidden_otherUser() throws Exception {
        mockMvc.perform(delete("/api/users/999")
                        .with(auth(1L))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    assertThat(result.getResolvedException()).isInstanceOf(ResponseStatusException.class);
                    ResponseStatusException ex = (ResponseStatusException) result.getResolvedException();
                    assertThat(ex.getStatusCode().value()).isEqualTo(403);
                });

        verifyNoInteractions(userService);
    }
}
