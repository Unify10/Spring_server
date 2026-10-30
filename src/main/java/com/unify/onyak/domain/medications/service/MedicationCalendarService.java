package com.unify.onyak.domain.medications.service;

import com.unify.onyak.domain.medications.dto.response.CalendarResponse;
import com.unify.onyak.global.security.CustomUserDetails;

import java.time.YearMonth;

public interface MedicationCalendarService {
    CalendarResponse getCalendar(CustomUserDetails user, Long wardId, YearMonth month);
}