package com.yoon.pickz.domain.auth.dto;

import com.yoon.pickz.domain.user.entity.enums.UserType;

public record SignUpResponse(
    Long userId,
    String email,
    UserType userType
) {}
