package com.unify.onyak.domain.users.auth.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    AUTH_ID_NOTFOUND(HttpStatus.NOT_FOUND, "존재하는 아이디가 없습니다."),
    AUTH_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    AUTH_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    AUTH_EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "인증 메일 발송에 실패했습니다."),
    AUTH_EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않거나 만료되었습니다."),
    AUTH_EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았습니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    AUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
