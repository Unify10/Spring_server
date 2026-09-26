package com.unify.onyak.domain.wards.auth.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WardAuthErrorCode implements ErrorCode {

    AUTH_ID_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 보호자 ID가 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
