package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.request.MedicationCreateRequest;
import com.unify.onyak.domain.medications.dto.request.MedicationUpdateRequest;
import com.unify.onyak.domain.medications.dto.request.ScheduleRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationResponse;
import com.unify.onyak.domain.medications.dto.response.ScheduleResponse;
import com.unify.onyak.domain.medications.service.MedicationService;
import com.unify.onyak.global.config.SecurityConfig;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicationController.class)
@Import(SecurityConfig.class)
class MedicationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MedicationService medicationService;

    static final MedicationResponse TYLENOL = new MedicationResponse(
            10L, "195700020", "타이레놀정500mg", List.of("아세트아미노펜"), "1정",
            LocalDate.of(2026, 10, 1), null, true, List.of("08:00", "20:00"), true, null);

    static RequestPostProcessor guardian() {
        CustomUserDetails user = new CustomUserDetails(3L, "3", null, "ROLE_GUARDIAN");
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        int status = mockMvc.perform(get("/wards/7/medications")).andReturn().getResponse().getStatus();
        assertTrue(status == 401 || status == 403, "status=" + status);
        verifyNoInteractions(medicationService);
    }

    // ── 5.1 ─────────────────────────────────────────────────────
    @Test
    void 약장_목록을_조회한다() throws Exception {
        when(medicationService.getMedications(any(), eq(7L), eq("active"))).thenReturn(List.of(TYLENOL));

        mockMvc.perform(get("/wards/7/medications").with(guardian()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].medication_id").value(10))
                .andExpect(jsonPath("$.data[0].drug_id").value("195700020"))
                .andExpect(jsonPath("$.data[0].schedule_times[0]").value("08:00"));
    }

    @Test
    void status_파라미터를_서비스에_넘긴다() throws Exception {
        when(medicationService.getMedications(any(), eq(7L), eq("stopped"))).thenReturn(List.of());

        mockMvc.perform(get("/wards/7/medications").param("status", "stopped").with(guardian()))
                .andExpect(status().isOk());
        verify(medicationService).getMedications(any(), eq(7L), eq("stopped"));
    }

    @Test
    void 약을_담으면_201() throws Exception {
        when(medicationService.addMedication(any(), eq(7L), any(MedicationCreateRequest.class))).thenReturn(TYLENOL);

        mockMvc.perform(post("/wards/7/medications").with(guardian())
                        .contentType("application/json")
                        .content("""
                                {"item_seq":"195700020","dosage":"1정","schedule_times":["08:00","20:00"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.drug_name").value("타이레놀정500mg"));
    }

    @Test
    void 용량이_없으면_400() throws Exception {
        mockMvc.perform(post("/wards/7/medications").with(guardian())
                        .contentType("application/json")
                        .content("{\"item_seq\":\"195700020\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
        verifyNoInteractions(medicationService);
    }

    @Test
    void 약을_담을_때도_시각_형식을_검사한다() throws Exception {
        mockMvc.perform(post("/wards/7/medications").with(guardian())
                        .contentType("application/json")
                        .content("{\"item_seq\":\"195700020\",\"dosage\":\"1정\",\"schedule_times\":[\"8시\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(medicationService);
    }

    @Test
    void 약장_항목을_조회한다() throws Exception {
        when(medicationService.getMedication(any(), eq(10L))).thenReturn(TYLENOL);

        mockMvc.perform(get("/medications/10").with(guardian()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medication_id").value(10));
    }

    // ── 5.2 ─────────────────────────────────────────────────────
    @Test
    void 복용을_중단한다() throws Exception {
        MedicationResponse stopped = new MedicationResponse(10L, "195700020", "타이레놀정500mg", List.of(), "1정",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 13), false, List.of(), true, null);
        when(medicationService.updateMedication(any(), eq(10L), any(MedicationUpdateRequest.class))).thenReturn(stopped);

        mockMvc.perform(patch("/medications/10").with(guardian())
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.end_date").value("2026-10-13"));
    }

    @Test
    void 삭제하면_204() throws Exception {
        mockMvc.perform(delete("/medications/10").with(guardian()))
                .andExpect(status().isNoContent());
        verify(medicationService).deleteMedication(any(), eq(10L));
    }

    // ── 5.3 ─────────────────────────────────────────────────────
    @Test
    void 일정을_조회한다() throws Exception {
        when(medicationService.getSchedule(any(), eq(10L)))
                .thenReturn(new ScheduleResponse(10L, List.of("08:00"), true, true));

        mockMvc.perform(get("/medications/10/schedule").with(guardian()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reminder_active").value(true));
    }

    @Test
    void 일정을_설정한다() throws Exception {
        when(medicationService.updateSchedule(any(), eq(10L), any(ScheduleRequest.class)))
                .thenReturn(new ScheduleResponse(10L, List.of("08:00", "20:00"), true, true));

        mockMvc.perform(put("/medications/10/schedule").with(guardian())
                        .contentType("application/json")
                        .content("{\"schedule_times\":[\"20:00\",\"08:00\"],\"push_enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schedule_times[1]").value("20:00"));
    }

    @Test
    void 시각_형식이_틀리면_400() throws Exception {
        mockMvc.perform(put("/medications/10/schedule").with(guardian())
                        .contentType("application/json")
                        .content("{\"schedule_times\":[\"25:00\"],\"push_enabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
        verifyNoInteractions(medicationService);
    }

    @Test
    void 같은_시각이_두번이면_400() throws Exception {
        mockMvc.perform(put("/medications/10/schedule").with(guardian())
                        .contentType("application/json")
                        .content("{\"schedule_times\":[\"08:00\",\"08:00\"],\"push_enabled\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(medicationService);
    }

    @Test
    void push_enabled가_없으면_400() throws Exception {
        mockMvc.perform(put("/medications/10/schedule").with(guardian())
                        .contentType("application/json")
                        .content("{\"schedule_times\":[\"08:00\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(medicationService);
    }
}
