package com.yoon.pickz.common.exception;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yoon.pickz.common.response.ApiError;
import com.yoon.pickz.common.response.ApiResponse;
import com.yoon.pickz.domain.auth.exception.AuthErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ApiError error = new ApiError(
            ex.getCode(),
            ex.getMessage(),
            ex.getDetails(),
            generateTraceId()
        );
        return ResponseEntity.status(ex.getStatus())
            .body(ApiResponse.error(error, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .toList();

        AuthErrorCode errorCode = AuthErrorCode.INVALID_REQUEST;
        ApiError error = new ApiError(
            errorCode.getCode(),
            errorCode.getMessage(),
            details,
            generateTraceId()
        );
        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponse.error(error, errorCode.getMessage()));
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
