package com.yoon.pickz.domain.user.dto;

import com.yoon.pickz.domain.user.entity.enums.UserStatus;
import com.yoon.pickz.domain.user.entity.enums.UserType;

public final class UserDto {

    private UserDto() {}

    public record UpdateMeRequest(
        String nickname,
        String profileImageUrl
    ) {}

    public record MeResponse(
        Long userId,
        String email,
        String nickname,
        UserType userType,
        UserStatus status,
        String profileImageUrl
    ) {}
}
