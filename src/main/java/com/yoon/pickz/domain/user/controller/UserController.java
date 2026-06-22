package com.yoon.pickz.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yoon.pickz.common.response.ApiResponse;
import com.yoon.pickz.domain.user.dto.UserDto;
import com.yoon.pickz.domain.user.service.UserService;
import com.yoon.pickz.infra.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.MeResponse>> getMe(
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        UserDto.MeResponse response = userService.getMe(user.userId());
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.MeResponse>> updateMe(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody UserDto.UpdateMeRequest request
    ) {
        UserDto.MeResponse response = userService.updateMe(user.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        userService.withdraw(user.userId());
        return ResponseEntity.ok(ApiResponse.success(null, null));
    }
}
