package com.unify.onyak.domain.medications.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MedicationCheckRequest(
        @NotNull(message = "복용 시각은 필수입니다.")
        @Pattern(regexp = ScheduleRequest.TIME_REGEX, message = "복용 시각은 HH:mm 형식이어야 합니다.")
        String scheduleTime
) {
}