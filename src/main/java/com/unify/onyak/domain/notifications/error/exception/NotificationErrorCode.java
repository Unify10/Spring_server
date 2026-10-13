package com.unify.onyak.domain.notifications.error.exception;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    WARD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 피보호자를 찾을 수 없습니다."),
    INVALID_RECIPIENT_ROLE(HttpStatus.FORBIDDEN, "알림을 조회할 수 없는 사용자 유형입니다.");

    private final HttpStatus httpStatus;
    private final String message;

}
