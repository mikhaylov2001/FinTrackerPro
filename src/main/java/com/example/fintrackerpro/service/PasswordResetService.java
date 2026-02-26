package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.user.PasswordResetToken;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.repository.PasswordResetTokenRepository;
import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService implements PasswordResetServiceBase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int TOKEN_EXPIRY_MINUTES = 60;
    private static final int TOKEN_LENGTH = 32; // байты

    @Transactional
    public void initiatePasswordReset(String email, String frontendBaseUrl) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Всегда одинаковый ответ (security best practice)
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Удаляем все старые токены этого юзера
        tokenRepository.deleteByUserId(user.getId());

        // Генерируем случайный токен
        String token = generateSecureToken();
        String tokenHash = hashToken(token);

        // Сохраняем в БД
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash(tokenHash)
            .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
            .build();

        tokenRepository.save(resetToken);

        // Формируем ссылку
        String resetLink = String.format("%s/reset-password?token=%s", frontendBaseUrl, token);

        // Отправляем письмо
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUserName(), resetLink);

        log.info("Password reset email sent to: {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = hashToken(token);

        PasswordResetToken resetToken = tokenRepository
            .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(tokenHash, LocalDateTime.now())
            .orElseThrow(() -> new IllegalArgumentException("Некорректный или истёкший токен"));

        User user = userRepository.findById(resetToken.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        // Обновляем пароль
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Помечаем токен как использованный
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
