package com.unify.onyak.domain.medications.dto.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * PATCH /medications/{medId} — 보낸(null 이 아닌) 필드만 바꾼다.
 * <ul>
 *   <li>중단: {"active": false, "end_date": "2026-10-13"} (end_date 생략 시 오늘)</li>
 *   <li>재개: {"active": true}</li>
 *   <li>용량 변경: {"dosage": "2정"}</li>
 * </ul>
 */
public record MedicationUpdateRequest(
        @Size(min = 1, max = 100)
        String dosage,

        Boolean active,

        LocalDate endDate
) {
}
