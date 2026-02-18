package com.example.fintrackerpro.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 64)
    private String firstName;

    @Size(max = 64)
    private String lastName;
}
