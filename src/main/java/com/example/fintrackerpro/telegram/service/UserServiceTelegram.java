package com.example.fintrackerpro.telegram.service;

import com.example.fintrackerpro.telegram.http.FinTrackerApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Slf4j
public class UserServiceTelegram {
    private final FinTrackerApiClient apiClient;

    public UserServiceTelegram(FinTrackerApiClient apiClient) {
        this.apiClient = apiClient;
    }
    // Получает или регистрирует пользователя
    public UserData getOrRegisterUser(Long chatId, String firstName, String lastName) {
        String fullName = buildFullName(firstName, lastName);

        try {
            FinTrackerApiClient.UserDto user = apiClient.getUserByChatId(chatId);
            log.info("✅ Пользователь найден в БД: {}", user.userName());

            return UserData.builder()
                    .isNewUser(false)
                    .user(user)
                    .displayName(user.userName())
                    .build();

        } catch (HttpClientErrorException.NotFound exception) {
            log.info("📝 Регистрация нового пользователя: {} (chatId: {})", fullName, chatId);

            FinTrackerApiClient.UserDto newUser = apiClient.registerUser(chatId, fullName);

            return UserData.builder()
                    .isNewUser(true)
                    .user(newUser)
                    .displayName(newUser.userName())
                    .build();
        }
    }

    // Получает пользователя по ID чата
    public FinTrackerApiClient.UserDto getUserByChatId(Long chatId) {
        return apiClient.getUserByChatId(chatId);
    }

    // Составляет полное имя
    private String buildFullName(String firstName, String lastName) {
        StringBuilder fullName = new StringBuilder();

        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }

        if (lastName != null && !lastName.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }

        return fullName.toString().isEmpty() ? "User" : fullName.toString();
    }

    // DTO класс для передачи данных
    public static class UserData {
        private final boolean isNewUser;
        private final FinTrackerApiClient.UserDto user;
        private final String displayName;

        public UserData(boolean isNewUser, FinTrackerApiClient.UserDto user, String displayName) {
            this.isNewUser = isNewUser;
            this.user = user;
            this.displayName = displayName;
        }

        public boolean isNewUser() {
            return isNewUser;
        }

        public FinTrackerApiClient.UserDto getUser() {
            return user;
        }

        public String getDisplayName() {
            return displayName;
        }

        // Builder для удобного создания
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean isNewUser;
            private FinTrackerApiClient.UserDto user;
            private String displayName;

            public Builder isNewUser(boolean isNewUser) {
                this.isNewUser = isNewUser;
                return this;
            }

            public Builder user(FinTrackerApiClient.UserDto user) {
                this.user = user;
                return this;
            }

            public Builder displayName(String displayName) {
                this.displayName = displayName;
                return this;
            }

            public UserData build() {
                return new UserData(isNewUser, user, displayName);
            }
        }
    }
}
