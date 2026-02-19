// UserSettingsService.java
package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.user.UserSettingsDto;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserSettingsDto getSettings(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        UserSettingsDto dto = new UserSettingsDto();
        dto.setDisplayCurrency(user.getDisplayCurrency());
        dto.setHideAmounts(user.isHideAmounts());
        return dto;
    }

    @Transactional
    public UserSettingsDto updateSettings(Long userId, UserSettingsDto dto) {
        User user = userRepository.findById(userId).orElseThrow();
        if (dto.getDisplayCurrency() != null) {
            user.setDisplayCurrency(dto.getDisplayCurrency());
        }
        user.setHideAmounts(dto.isHideAmounts());
        userRepository.save(user);
        return getSettings(userId);
    }
}
