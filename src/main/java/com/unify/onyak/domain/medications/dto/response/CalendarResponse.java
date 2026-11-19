package com.unify.onyak.domain.medications.dto.response;

import java.util.List;

public record CalendarResponse(
        Long wardId,
        String month,
        List<CalendarDayResponse> days
) {
}
