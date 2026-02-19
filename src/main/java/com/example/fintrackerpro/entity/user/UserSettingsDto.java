// UserSettingsDto.java
package com.example.fintrackerpro.entity.user;

import lombok.Data;

@Data
public class UserSettingsDto {
    private String displayCurrency; // "RUB", "USD", "EUR"
    private boolean hideAmounts;
}
