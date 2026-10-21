package com.unify.onyak.domain.wards.auth.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WardAuthErrorCode implements ErrorCode {
    
    GUARDIAN_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "보호자 권한이 필요합니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
