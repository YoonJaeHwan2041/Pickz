package com.yoon.pickz.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yoon.pickz.common.exception.BusinessException;
import com.yoon.pickz.domain.auth.dto.AuthDto;
import com.yoon.pickz.domain.auth.entity.RefreshToken;
import com.yoon.pickz.domain.auth.exception.AuthErrorCode;
import com.yoon.pickz.domain.auth.repository.RefreshTokenRepository;
import com.yoon.pickz.domain.user.entity.User;
import com.yoon.pickz.domain.user.entity.enums.UserStatus;
import com.yoon.pickz.domain.user.repository.UserRepository;
import com.yoon.pickz.infra.security.JwtProvider;
import com.yoon.pickz.infra.security.JwtProvider.TokenIssueResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public LoginResult login(AuthDto.LoginRequest request) {
        if (!EMAIL_PATTERN.matcher(request.email()).matches()) {
            throw new BusinessException(AuthErrorCode.INVALID_EMAIL_FORMAT);
        }

        User user = userRepository.findByEmail(request.email())
            .filter(found -> found.getStatus() == UserStatus.ACTIVE)
            .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
            .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        TokenIssueResult accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());
        TokenIssueResult refreshToken = jwtProvider.generateRefreshToken(user.getId());

        RefreshToken refreshTokenEntity = new RefreshToken(
            user.getId(),
            hashToken(refreshToken.token()),
            refreshToken.expiresAt()
        );
        refreshTokenRepository.save(refreshTokenEntity);

        AuthDto.LoginResponse response = new AuthDto.LoginResponse(
            accessToken.token(),
            accessToken.expiresAt()
        );

        return new LoginResult(response, refreshToken.token());
    }

    @Transactional
    public AuthDto.RefreshResponse refresh(String rawRefreshToken) {
        // 서명 검증
        Long userId = jwtProvider.parseRefreshTokenUserId(rawRefreshToken);
        if (userId == null) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // DB에서 해시로 조회 후 유효성 확인
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .filter(RefreshToken::isValid)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        User user = userRepository.findById(userId)
            .filter(found -> found.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        // 기존 토큰 폐기 후 새 Access Token 발급
        refreshToken.revoke();

        TokenIssueResult accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail());

        return new AuthDto.RefreshResponse(accessToken.token(), accessToken.expiresAt());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record LoginResult(AuthDto.LoginResponse response, String refreshToken) {}
}
