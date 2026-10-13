package com.unify.onyak.domain.medications.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;

/** PUT /medications/{medId}/schedule — 복용 시각·알림 전체 교체. */
public record ScheduleRequest(
        @NotNull
        @Size(max = 12, message = "복용 시각은 최대 12개까지 입력할 수 있습니다.")
        List<@NotNull @Pattern(regexp = TIME_REGEX, message = "복용 시각은 HH:mm 형식이어야 합니다.") String> scheduleTimes,

        @NotNull
        Boolean pushEnabled
) {
    public static final String TIME_REGEX = "^([01]\\d|2[0-3]):[0-5]\\d$";

    @AssertTrue(message = "같은 복용 시각이 두 번 들어 있습니다.")
    public boolean isScheduleTimesUnique() {
        return isUnique(scheduleTimes);
    }

    static boolean isUnique(List<String> times) {
        return times == null || new HashSet<>(times).size() == times.size();
    }
}
