package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.entity.user.UserRegistrationRequest;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.exception.UserFoundException;
import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public User registerUser(UserRegistrationRequest request) {
        // Проверка на дубликат email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Проверка на дубликат userName
        if (userRepository.findByUserName(request.getUserName()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        // Создаём пользователя
        User user = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .chatId(request.getChatId())
                .build();

        return userRepository.save(user);
    }


    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id={}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });
    }

    /**
     * Получает пользователя по Telegram Chat ID
     */
    public User getUserByChatId(Long chatId) {
        return userRepository.findByChatId(chatId)
                .orElseThrow(() -> {
                    log.error("User not found with chatId={}", chatId);
                    return new ResourceNotFoundException("User not found with chatId: " + chatId);
                });
    }


    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserFoundException("User not found" + userId);
        }
        userRepository.deleteById(userId);
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserByUserName(String userName) {
        return userRepository.findByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userName));
    }

    public User updateUser(Long userId, User updatedUser) {
        User user = getUserById(userId);

        if (updatedUser.getEmail() != null) {
            user.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getChatId() != null) {
            user.setChatId(updatedUser.getChatId());
        }

        log.info("✅ User updated: id={}", userId);
        return userRepository.save(user);
    }



    public User registerUserViaGoogle(String email, String googleId, String name) {
        log.info("📝 Registering new user via Google: email={}, name={}", email, name);

        User user = new User();
        user.setEmail(email);
        user.setUserName(name != null ? name : email.split("@")[0]); // ✅ ВАЖНО!
        user.setGoogleId(googleId);
        user.setPassword(""); // Google не требует пароль

        User savedUser = userRepository.save(user);
        log.info("✅ User saved: id={}, userName={}", savedUser.getId(), savedUser.getUserName());

        return savedUser;
    }



}

