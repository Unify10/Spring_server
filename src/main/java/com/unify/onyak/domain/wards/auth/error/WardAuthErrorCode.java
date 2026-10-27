package com.unify.onyak.domain.wards.auth.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WardAuthErrorCode implements ErrorCode {
    
    GUARDIAN_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "보호자 권한이 필요합니다."),
    TOO_MANY_LOGIN_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도 제한 5회를 초과하셨습니다. 5분 후에 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}
