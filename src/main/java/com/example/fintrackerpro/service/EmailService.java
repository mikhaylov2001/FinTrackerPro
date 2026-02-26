package com.example.fintrackerpro.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String username, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Восстановление пароля — FinTracker");
            message.setText(buildResetEmailText(username, resetLink));
            message.setFrom("noreply@fintracker.com"); // замени на свой

            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Не удалось отправить письмо");
        }
    }

    private String buildResetEmailText(String username, String resetLink) {
        return String.format(
            "Здравствуйте, %s!\n\n" +
            "Вы запросили восстановление пароля для своего аккаунта в FinTracker.\n\n" +
            "Для сброса пароля перейдите по ссылке:\n%s\n\n" +
            "Ссылка действительна в течение 60 минут.\n\n" +
            "Если это были не вы, просто проигнорируйте это письмо — ваш пароль останется без изменений.\n\n" +
            "С уважением,\nКоманда FinTracker",
            username,
            resetLink
        );
    }

}
