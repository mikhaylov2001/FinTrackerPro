package com.example.fintrackerpro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEmailRequest {

    @Email
    @NotBlank
    private String newEmail;

    @NotBlank
    private String password;
}