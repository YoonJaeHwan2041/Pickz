package com.yoon.pickz.domain.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yoon.pickz.common.exception.BusinessException;
import com.yoon.pickz.domain.auth.dto.AuthDto;
import com.yoon.pickz.domain.auth.exception.AuthErrorCode;
import com.yoon.pickz.domain.user.entity.User;
import com.yoon.pickz.domain.user.entity.enums.UserType;
import com.yoon.pickz.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
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
}
