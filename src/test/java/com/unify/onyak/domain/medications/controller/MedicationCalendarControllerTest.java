package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.response.CalendarDayResponse;
import com.unify.onyak.domain.medications.dto.response.CalendarResponse;
import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.service.MedicationCalendarService;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicationCalendarController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class MedicationCalendarControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean MedicationCalendarService medicationCalendarService;

    static RequestPostProcessor loginAs(long userId, String role) {
        CustomUserDetails user = new CustomUserDetails(userId, String.valueOf(userId), null, role);
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void 미인증_요청은_401이다() throws Exception {
        mockMvc.perform(get("/wards/7/calendar"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(medicationCalendarService);
    }

    @Test
    void 월을_지정해_달력을_조회한다() throws Exception {
        when(medicationCalendarService.getCalendar(any(), eq(7L), eq(YearMonth.of(2026, 10))))
                .thenReturn(new CalendarResponse(7L, "2026-10",
                        List.of(new CalendarDayResponse(LocalDate.of(2026, 10, 1), 3, 2, 66.7))));

        mockMvc.perform(get("/wards/7/calendar").param("month", "2026-10")
                        .with(loginAs(3L, "ROLE_GUARDIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ward_id").value(7))
                .andExpect(jsonPath("$.data.month").value("2026-10"))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$.data.days[0].scheduled").value(3))
                .andExpect(jsonPath("$.data.days[0].completed").value(2))
                .andExpect(jsonPath("$.data.days[0].rate").value(66.7));
    }

    @Test
    void 월을_생략하면_null로_서비스에_전달한다() throws Exception {
        when(medicationCalendarService.getCalendar(any(), eq(7L), eq(null)))
                .thenReturn(new CalendarResponse(7L, "2026-10", List.of()));

        mockMvc.perform(get("/wards/7/calendar").with(loginAs(7L, "ROLE_WARD")))
                .andExpect(status().isOk());

        verify(medicationCalendarService).getCalendar(any(), eq(7L), eq(null));
    }

    @Test
    void 월_형식이_틀리면_400이고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(get("/wards/7/calendar").param("month", "2026-13")
                        .with(loginAs(3L, "ROLE_GUARDIAN")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicationCalendarService);
    }

    @Test
    void 권한이_없으면_403이다() throws Exception {
        when(medicationCalendarService.getCalendar(any(), eq(7L), any()))
                .thenThrow(new WardAccessDeniedException());

        mockMvc.perform(get("/wards/7/calendar").param("month", "2026-10")
                        .with(loginAs(99L, "ROLE_GUARDIAN")))
                .andExpect(status().isForbidden());
    }
}