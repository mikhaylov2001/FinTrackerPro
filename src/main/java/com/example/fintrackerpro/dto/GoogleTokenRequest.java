package com.example.fintrackerpro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Google ID Token для аутентификации")

public class GoogleTokenRequest {
    @Schema(
            description = "Google ID Token полученный от Google Sign-In",
            example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE4MmU0NTBhNDJkYjMwYjdhZmZhMD...",
            required = true
    )
    private String idToken;
}
