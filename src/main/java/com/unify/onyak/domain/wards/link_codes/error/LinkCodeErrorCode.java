package com.unify.onyak.domain.wards.link_codes.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LinkCodeErrorCode implements ErrorCode {

    INVALID_LINK_CODE_REQUEST(HttpStatus.BAD_REQUEST, "유효한 연동 코드 또는 QR 토큰이 없거나 만료되었습니다."),
    LINK_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"연동 코드 생성에 실패하였습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}
