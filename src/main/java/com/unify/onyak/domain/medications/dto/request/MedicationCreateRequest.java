package com.unify.onyak.domain.medications.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** POST /wards/{wardId}/medications — 약장에 약 담기. */
public record MedicationCreateRequest(
        // /drugs/search·/drugs/barcode·/drugs/analyze(FastAPI) 가 돌려준 품목기준코드
        @NotBlank
        @Size(max = 20)
        String itemSeq,

        @NotBlank
        @Size(max = 100)
        String dosage,

        // 생략하면 오늘(KST)
        LocalDate startDate,

        @Size(max = 12, message = "복용 시각은 최대 12개까지 입력할 수 있습니다.")
        List<@Pattern(regexp = ScheduleRequest.TIME_REGEX, message = "복용 시각은 HH:mm 형식이어야 합니다.") String> scheduleTimes,

        // 생략하면 true
        Boolean pushEnabled
) {
    @AssertTrue(message = "같은 복용 시각이 두 번 들어 있습니다.")
    public boolean isScheduleTimesUnique() {
        return ScheduleRequest.isUnique(scheduleTimes);
    }
}
