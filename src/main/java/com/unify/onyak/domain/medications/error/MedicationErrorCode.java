package com.unify.onyak.domain.medications.error;

import com.unify.onyak.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MedicationErrorCode implements ErrorCode {
    WARD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 피보호자를 찾을 수 없습니다."),
    WARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 피보호자의 약장에 접근할 권한이 없습니다."),
    MEDICATION_WRITE_FORBIDDEN(HttpStatus.FORBIDDEN, "약 담기·중단·재개·용량 변경·삭제는 보호자만 할 수 있습니다."),
    MEDICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 약장 항목을 찾을 수 없습니다."),
    MEDICATION_DUPLICATE(HttpStatus.CONFLICT, "이미 복용 중으로 등록된 약입니다."),
    MEDICATION_INACTIVE(HttpStatus.CONFLICT, "복용 중단된 약입니다. 먼저 복용을 재개해 주세요."),
    MEDICATION_INVALID_END_DATE(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다."),
    MEDICATION_END_DATE_ON_ACTIVE(HttpStatus.BAD_REQUEST, "복용 중인 약에는 종료일만 따로 지정할 수 없습니다. 중단(active=false)과 함께 보내 주세요."),
    MEDICATION_INVALID_STATUS_FILTER(HttpStatus.BAD_REQUEST, "status 는 active, stopped, all 중 하나입니다."),
    SCHEDULE_TIME_INVALID(HttpStatus.BAD_REQUEST, "복용 시각은 HH:mm 형식으로 중복 없이 최대 12개까지 입력할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
