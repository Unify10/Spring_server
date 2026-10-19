package com.unify.onyak.domain.drugs.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DrugErrorCode implements ErrorCode {
    DRUG_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 품목기준코드의 의약품을 찾을 수 없습니다."),
    DRUG_API_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "의약품 정보 서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String message;
}
