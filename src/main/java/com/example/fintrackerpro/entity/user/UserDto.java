package com.example.fintrackerpro.entity.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    @JsonProperty("userName")  // ✅ Явно указываем имя в JSON
    private String userName;
    private String email;
}