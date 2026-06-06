package com.yoon.pickz.domain.auth.dto;

import com.yoon.pickz.domain.user.entity.enums.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AuthDto {

    private AuthDto() {
    }

    public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String nickname,
        @NotNull UserType userType
    ) {}

    public record SignUpResponse(
        Long userId,
        String email,
        UserType userType
    ) {}
}
