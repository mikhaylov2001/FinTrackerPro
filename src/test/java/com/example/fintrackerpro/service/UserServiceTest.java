package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest registrationRequest;
    private User user;

    @BeforeEach
    void setUp() {
        // ✅ ВАЖНО: Внедряем passwordEncoder вручную через Reflection
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);

        // Создаём тестовые данные
        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setUserName("testuser");
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setUserName("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
    }

    @Test
    @DisplayName("Регистрация пользователя - успешно")
    void registerUser_Success() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUserName(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        User result = userService.registerUser(registrationRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserName()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).findByUserName("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация - username уже существует")
    void registerUser_UsernameExists_ThrowsException() {
        // Given
        User existingUser = new User();
        existingUser.setUserName("testuser");

        // ✅ Email свободен (проходит первую проверку)
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        // ✅ Username занят (вторая проверка падает)
        when(userRepository.findByUserName("testuser"))
                .thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");

        // Проверяем что обе проверки были вызваны
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).findByUserName("testuser");
        // Но save не вызывался
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Получить пользователя по ID - успешно")
    void getUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        User result = userService.getUserById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Получить пользователя по ID - не найден")
    void getUserById_NotFound_ThrowsException() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Получить пользователя по username - успешно")
    void getUserByUserName_Success() {
        // Given
        when(userRepository.findByUserName("testuser")).thenReturn(Optional.of(user));

        // When
        User result = userService.getUserByUserName("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserName()).isEqualTo("testuser");
        verify(userRepository).findByUserName("testuser");
    }

    @Test
    @DisplayName("Получить пользователя по username - не найден")
    void getUserByUserName_NotFound_ThrowsException() {
        // Given
        when(userRepository.findByUserName(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUserName("nonexistent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }
}
