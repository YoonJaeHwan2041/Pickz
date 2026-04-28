package com.yoon.pickz.common.response;

import java.time.Instant;

public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    ApiError error,
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, null, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ApiError error, String message) {
        return new ApiResponse<>(false, null, message, error, Instant.now());
    }
}