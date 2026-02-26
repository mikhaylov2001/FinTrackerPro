package com.example.fintrackerpro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);


    public void sendPasswordResetEmail(String to, String username, String resetLink) {
        String body = buildResetEmailText(username, resetLink);

        log.info("FAKE PASSWORD RESET EMAIL to={}", to);
        log.info("FAKE PASSWORD RESET EMAIL BODY:\n{}", body);
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
