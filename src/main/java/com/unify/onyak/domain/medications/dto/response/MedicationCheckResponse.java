package com.unify.onyak.domain.medications.dto.response;

import com.unify.onyak.domain.medications.entity.MedicationCheck;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public record MedicationCheckResponse(
        Long id,
        Long medicationId,
        String scheduleTime,
        OffsetDateTime checkedAt
) {
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public static MedicationCheckResponse from(MedicationCheck check) {
        return new MedicationCheckResponse(
                check.getId(),
                check.getMedication().getId(),
                HH_MM.format(check.getScheduleTime()),
                check.getCheckedAt()
        );
    }
}