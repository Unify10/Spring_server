package com.unify.onyak.domain.users.withdraw.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WithdrawErrorCode implements ErrorCode {
    WITHDRAW_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    WITHDRAW_FORBIDDEN(HttpStatus.FORBIDDEN, "보호자만 회원 탈퇴를 할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}