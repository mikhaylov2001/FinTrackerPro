package com.example.fintrackerpro.dto;

import com.example.fintrackerpro.entity.user.UserDto;
import lombok.Builder;

@Builder
    public record AuthResponse(
            String token,
            PublicUserDto user
    ) {

}



