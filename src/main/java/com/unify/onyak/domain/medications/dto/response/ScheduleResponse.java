package com.unify.onyak.domain.medications.dto.response;

import com.unify.onyak.domain.medications.entity.Medication;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ScheduleResponse(
        Long medicationId,
        List<String> scheduleTimes,
        boolean pushEnabled,
        // 복용 중 && 약 알림 켬 && 피보호자 복약 알림 켬 && 시각 1개 이상
        boolean reminderActive
) {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public static ScheduleResponse from(Medication m) {
        return new ScheduleResponse(
                m.getId(),
                format(m.getScheduleTimes()),
                Boolean.TRUE.equals(m.getPushEnabled()),
                m.isReminderActive()
        );
    }

    static List<String> format(List<LocalTime> times) {
        return times.stream().sorted().map(HH_MM::format).toList();
    }
}
