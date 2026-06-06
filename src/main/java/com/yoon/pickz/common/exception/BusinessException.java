package com.yoon.pickz.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> details;

    public BusinessException(ErrorCodeEnum errorCode) {
        this(errorCode, null);
    }

    public BusinessException(ErrorCodeEnum errorCode, List<String> details) {
        super(errorCode.getMessage());
        this.status = errorCode.getHttpStatus();
        this.code = errorCode.getCode();
        this.details = details;
    }

    public interface ErrorCodeEnum {
        HttpStatus getHttpStatus();
        String getCode();
        String getMessage();
    }
}
