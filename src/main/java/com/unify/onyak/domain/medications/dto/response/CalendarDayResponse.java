package com.unify.onyak.domain.medications.dto.response;

import java.time.LocalDate;

public record CalendarDayResponse(
        LocalDate date,
        int scheduled,
        int completed,
        Double rate
) {
}