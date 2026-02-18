package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.UpdateProfileRequest;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserDto;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.exception.UserFoundException;
import com.example.fintrackerpro.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // РЕГИСТРАЦИЯ: firstName + lastName + email + password
    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        String generatedUserName = request.getEmail().split("@")[0];

        User user = User.builder()
                .userName(generatedUserName)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        return userRepository.save(user);
    }

    public User getUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });
    }

    public UserDto getUserById(Long userId) {
        return toDto(getUserEntityById(userId));
    }

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserFoundException("User not found " + userId);
        }
        userRepository.deleteById(userId);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    // ДЛЯ ЛОГИНА: получаем по email
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public UserDto updateUser(Long userId, User updatedUser) {
        User user = getUserEntityById(userId);

        if (updatedUser.getEmail() != null) {
            user.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getChatId() != null) {
            user.setChatId(updatedUser.getChatId());
        }

        log.info("✅ User updated: id={}", userId);
        return toDto(userRepository.save(user));
    }

    public User registerUserViaGoogle(String email, String googleId, String name) {
        log.info("📝 Registering new user via Google: email={}, name={}", email, name);

        User user = new User();
        user.setEmail(email);
        user.setUserName(name != null ? name : email.split("@")[0]);
        user.setGoogleId(googleId);
        user.setPassword("");

        User savedUser = userRepository.save(user);
        log.info("✅ User saved: id={}, userName={}", savedUser.getId(), savedUser.getUserName());

        return savedUser;
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUserName(),
                user.getEmail()
        );
    }

    public void changePassword(Long userId , String currentPassword, String newPassword){
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.error("User not found with id={}", userId);
            return new EntityNotFoundException("User not found with id: " + userId);
        });
        if (user.getGoogleId() != null && (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new ResponseStatusException(BAD_REQUEST, "Пароль нельзя изменить для Google-аккаунта");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Текущий пароль неверен");
        }
        if (newPassword.length() < 6) {
            throw new ResponseStatusException(BAD_REQUEST, "Новый пароль должен быть не короче 6 символов");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("🔐 Password changed for userId={}", userId);
    }

    public User updateProfile(Long userId, UpdateProfileRequest req) {
        User user = getUserEntityById(userId);
        if (req.getFirstName() != null) {
            user.setFirstName(req.getFirstName());
        }
        if (req.getLastName() != null) {
            user.setLastName(req.getLastName());
        }
        return userRepository.save(user);
    }

    public User changeEmail(Long userId, String newEmail, String password) {
        User user = getUserEntityById(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
        user.setEmail(newEmail);
        return userRepository.save(user);
    }

}
