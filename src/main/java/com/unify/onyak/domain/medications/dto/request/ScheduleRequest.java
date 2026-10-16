package com.unify.onyak.domain.medications.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;

/**
 * PATCH /medications/{medId}/schedule — 보낸 필드만 바꾼다.
 * <ul>
 *   <li>schedule_times: 보내면 목록 전체를 이 값으로 교체 (빈 배열이면 일정 해제)</li>
 *   <li>push_enabled: 보내면 알림 on/off 변경</li>
 * </ul>
 */
public record ScheduleRequest(
        @Size(max = 12, message = "복용 시각은 최대 12개까지 입력할 수 있습니다.")
        List<@NotNull @Pattern(regexp = TIME_REGEX, message = "복용 시각은 HH:mm 형식이어야 합니다.") String> scheduleTimes,

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
