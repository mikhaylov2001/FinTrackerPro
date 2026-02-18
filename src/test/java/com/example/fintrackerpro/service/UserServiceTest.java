package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserDto;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private UserRegistrationRequest registrationRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registrationRequest = new UserRegistrationRequest();
        registrationRequest.setFirstName("Test");
        registrationRequest.setLastName("User");
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setUserName("test"); // генерируется из email до @
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
    }

    @Test
    @DisplayName("Регистрация пользователя - успешно")
    void registerUser_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.registerUser(registrationRequest);

        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUserName()).isEqualTo("test");
        assertThat(result.getFirstName()).isEqualTo("Test");
        assertThat(result.getLastName()).isEqualTo("User");

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Регистрация пользователя - email уже занят")
    void registerUser_emailAlreadyExists_throws() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.registerUser(registrationRequest))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("getUserById (DTO) - успешно")
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto dto = userService.getUserById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUserName()).isEqualTo("test");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("getUserById (DTO) - пользователь не найден")
    void getUserById_notFound_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findById(999L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("getUserEntityById - успешно")
    void getUserEntityById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User entity = userService.getUserEntityById(1L);

        assertThat(entity.getId()).isEqualTo(1L);

        verify(userRepository).findById(1L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("getUserEntityById - пользователь не найден")
    void getUserEntityById_notFound_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserEntityById(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findById(999L);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }
}
