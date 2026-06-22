package com.yoon.pickz.domain.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yoon.pickz.common.exception.BusinessException;
import com.yoon.pickz.domain.auth.dto.AuthDto;
import com.yoon.pickz.domain.auth.exception.AuthErrorCode;
import com.yoon.pickz.domain.auth.repository.RefreshTokenRepository;
import com.yoon.pickz.domain.user.dto.UserDto;
import com.yoon.pickz.domain.user.entity.User;
import com.yoon.pickz.domain.user.entity.enums.UserType;
import com.yoon.pickz.domain.user.exception.UserErrorCode;
import com.yoon.pickz.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthDto.SignUpResponse signup(AuthDto.SignUpRequest request) {
        if (request.userType() != UserType.GENERAL && request.userType() != UserType.BUSINESS) {
            throw new BusinessException(
                AuthErrorCode.INVALID_REQUEST,
                List.of("userType must be GENERAL or BUSINESS")
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(
                AuthErrorCode.EMAIL_ALREADY_EXISTS,
                List.of("email must be unique")
            );
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(
                AuthErrorCode.NICKNAME_ALREADY_EXISTS,
                List.of("nickname must be unique")
            );
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, request.nickname(), request.userType());
        User saved = userRepository.save(user);

        return new AuthDto.SignUpResponse(saved.getId(), saved.getEmail(), saved.getUserType());
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 발급된 Refresh Token 전체 폐기
        refreshTokenRepository.findAllByUserId(userId)
            .forEach(token -> {
                if (token.isValid()) {
                    token.revoke();
                }
            });

        user.softDelete();
    }

    @Transactional
    public UserDto.MeResponse updateMe(Long userId, UserDto.UpdateMeRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.nickname())) {
                throw new BusinessException(UserErrorCode.USER_NICKNAME_ALREADY_EXISTS);
            }
            user.updateNickname(request.nickname());
        }

        if (request.profileImageUrl() != null) {
            user.updateProfileImageUrl(request.profileImageUrl());
        }

        return new UserDto.MeResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getUserType(),
            user.getStatus(),
            user.getProfileImageUrl()
        );
    }

    @Transactional(readOnly = true)
    public UserDto.MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new UserDto.MeResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getUserType(),
            user.getStatus(),
            user.getProfileImageUrl()
        );
    }
}
