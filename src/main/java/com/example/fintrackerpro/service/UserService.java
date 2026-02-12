package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserDto;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.exception.UserFoundException;
import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepository.findByUserName(request.getUserName()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .chatId(request.getChatId())
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

    public UserDto getUserByUserName(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userName));
        return toDto(user);
    }

    // UserService
    public User getUserEntityByUserName(String userName) {
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userName));
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
}
