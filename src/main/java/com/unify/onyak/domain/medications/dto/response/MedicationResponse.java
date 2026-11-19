package com.unify.onyak.domain.medications.dto.response;

import com.unify.onyak.domain.medications.entity.Medication;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record MedicationResponse(
        Long medicationId,
        String drugId,          // 품목기준코드(item_seq) — FastAPI /drugs/{drugId} 와 같은 값
        String drugName,
        List<String> ingredients,
        String dosage,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        List<String> scheduleTimes,
        boolean pushEnabled,
        OffsetDateTime createdAt
) {
    public static MedicationResponse from(Medication m) {
        return new MedicationResponse(
                m.getId(),
                m.getDrug().getItemSeq(),
                m.getDrug().getName(),
                List.copyOf(m.getDrug().getIngredients()),
                m.getDosage(),
                m.getStartDate(),
                m.getEndDate(),
                m.isActive(),
                ScheduleResponse.format(m.getScheduleTimes()),
                Boolean.TRUE.equals(m.getPushEnabled()),
                m.getCreatedAt()
        );
    }
}
