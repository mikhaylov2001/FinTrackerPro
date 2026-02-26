package com.example.fintrackerpro.service;

public interface PasswordResetServiceBase {
    void initiatePasswordReset(String email, String frontendBaseUrl);
    void resetPassword(String token, String newPassword);
}
