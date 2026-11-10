package com.unify.onyak.domain.wards.profile.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WardProfileErrorCode implements ErrorCode {
    WARD_NOT_FOUND(HttpStatus.NOT_FOUND, "피보호자를 찾지 못 하였습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
