package com.unify.onyak.domain.medications.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MedicationCheckErrorCode implements ErrorCode {
    CHECK_TIME_NOT_SCHEDULED(HttpStatus.BAD_REQUEST, "복용 일정에 없는 시각입니다."),
    CHECK_ALREADY_DONE(HttpStatus.CONFLICT, "이미 복용 완료로 체크한 시각입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}