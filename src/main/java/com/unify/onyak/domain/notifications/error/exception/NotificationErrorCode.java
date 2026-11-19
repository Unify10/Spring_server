package com.unify.onyak.domain.notifications.error.exception;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    WARD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 피보호자를 찾을 수 없습니다."),
    INVALID_RECIPIENT_ROLE(HttpStatus.FORBIDDEN, "알림을 조회할 수 없는 사용자 유형입니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "페이지 요청 값이 올바르지 않습니다."),
    SOS_FORBIDDEN(HttpStatus.FORBIDDEN, "SOS는 피보호자만 보낼 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

}
