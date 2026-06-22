package com.yoon.pickz.domain.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yoon.pickz.common.response.ApiResponse;
import com.yoon.pickz.domain.auth.dto.AuthDto;
import com.yoon.pickz.domain.auth.service.AuthService;
import com.yoon.pickz.domain.auth.service.AuthService.LoginResult;
import com.yoon.pickz.domain.user.service.UserService;
import com.yoon.pickz.infra.security.JwtProperties;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthDto.SignUpResponse>> signup(@Valid @RequestBody AuthDto.SignUpRequest request) {
        AuthDto.SignUpResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        LoginResult result = authService.login(request);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.refreshToken())
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(jwtProperties.getRefreshTokenValiditySeconds())
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(ApiResponse.success(result.response(), null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.RefreshResponse>> refresh(
        @CookieValue(name = "refreshToken") String refreshToken
    ) {
        AuthDto.RefreshResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(0)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
            .body(ApiResponse.success(null, null));
    }
}
