package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.response.CalendarResponse;
import com.unify.onyak.domain.medications.service.MedicationCalendarService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
public class MedicationCalendarController {

    private final MedicationCalendarService medicationCalendarService;

    // 5.5 복약 달력 — 보호자·피보호자 모두
    @GetMapping("/wards/{wardId}/calendar")
    public ApiResponse<CalendarResponse> getCalendar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wardId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return ApiResponse.ok(medicationCalendarService.getCalendar(userDetails, wardId, month));
    }
}