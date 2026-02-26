package com.example.fintrackerpro.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
@Slf4j
public class PasswordResetServiceStub implements PasswordResetServiceBase {

    @Override
    public void initiatePasswordReset(String email, String frontendBaseUrl) {
        log.info("Password reset requested for {} (stub in prod, email not sent)", email);
        // Можно ничего не делать или кидать бизнес-исключение
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        log.warn("Password reset via token is not supported in prod stub");
        throw new UnsupportedOperationException("Password reset is not available");
    }
}
