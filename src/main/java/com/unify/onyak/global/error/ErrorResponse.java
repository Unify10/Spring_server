package com.unify.onyak.global.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {
    private LocalDateTime timeStamp;
    private String errorCode;
    private int status;
    private String message;
    private List<FieldErrorDetail> errors;

    public static ErrorResponse from(CustomException e) {
        return ErrorResponse.builder()
                .timeStamp(LocalDateTime.now())
                .errorCode(e.getErrorCode().name())
                .status(e.getStatus().value())
                .message(e.getMessage())
                .build();
    }

    public static ErrorResponse of(HttpStatus status, String errorCode, String message) {
        return ErrorResponse.builder()
                .timeStamp(LocalDateTime.now())
                .errorCode(errorCode)
                .status(status.value())
                .message(message)
                .build();
    }

    // ApiResponse 에서 사용하기 위한 코드
    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode.getHttpStatus(), errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse ofValidation(List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .timeStamp(LocalDateTime.now())
                .errorCode("VALIDATION_ERROR")
                .status(HttpStatus.BAD_REQUEST.value())
                .message("입력값이 올바르지 않습니다.")
                .errors(fieldErrors.stream()
                        .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                        .toList())
                .build();
    }

    public record FieldErrorDetail(String field, String message) {
    }
}
