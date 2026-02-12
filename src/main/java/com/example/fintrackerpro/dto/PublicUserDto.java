package com.example.fintrackerpro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PublicUserDto(
        Long id,
        @JsonProperty("userName") String userName,
        String email
) {}