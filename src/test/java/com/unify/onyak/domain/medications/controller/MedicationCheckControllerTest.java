package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.response.MedicationCheckResponse;
import com.unify.onyak.domain.medications.error.exception.CheckAlreadyDoneException;
import com.unify.onyak.domain.medications.service.MedicationCheckService;
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

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicationCheckController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class MedicationCheckControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean MedicationCheckService medicationCheckService;

    static RequestPostProcessor loginAs(long userId, String role) {
        CustomUserDetails user = new CustomUserDetails(userId, String.valueOf(userId), null, role);
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        int status = mockMvc.perform(post("/medications/10/check")
                        .contentType("application/json")
                        .content("{\"schedule_time\":\"08:00\"}"))
                .andReturn().getResponse().getStatus();
        assertEquals(401, status);
        verifyNoInteractions(medicationCheckService);
    }

    @Test
    void 보호자가_복약_완료를_체크한다() throws Exception {
        when(medicationCheckService.check(any(), eq(10L), any()))
                .thenReturn(new MedicationCheckResponse(1L, 10L, "08:00", OffsetDateTime.now()));

        mockMvc.perform(post("/medications/10/check")
                        .with(loginAs(3L, "ROLE_GUARDIAN"))
                        .contentType("application/json")
                        .content("{\"schedule_time\":\"08:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.medication_id").value(10))
                .andExpect(jsonPath("$.data.schedule_time").value("08:00"));
    }

    @Test
    void 피보호자도_복약_완료를_체크할_수_있다() throws Exception {
        when(medicationCheckService.check(any(), eq(10L), any()))
                .thenReturn(new MedicationCheckResponse(2L, 10L, "20:00", OffsetDateTime.now()));

        mockMvc.perform(post("/medications/10/check")
                        .with(loginAs(7L, "ROLE_WARD"))
                        .contentType("application/json")
                        .content("{\"schedule_time\":\"20:00\"}"))
                .andExpect(status().isCreated());

        verify(medicationCheckService).check(any(), eq(10L), any());
    }

    @Test
    void 시각_형식이_틀리면_400이고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/medications/10/check")
                        .with(loginAs(3L, "ROLE_GUARDIAN"))
                        .contentType("application/json")
                        .content("{\"schedule_time\":\"25:99\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicationCheckService);
    }

    @Test
    void 시각이_없으면_400이다() throws Exception {
        mockMvc.perform(post("/medications/10/check")
                        .with(loginAs(3L, "ROLE_GUARDIAN"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(medicationCheckService);
    }

    @Test
    void 이미_체크한_시각이면_409이다() throws Exception {
        when(medicationCheckService.check(any(), eq(10L), any()))
                .thenThrow(new CheckAlreadyDoneException());

        mockMvc.perform(post("/medications/10/check")
                        .with(loginAs(3L, "ROLE_GUARDIAN"))
                        .contentType("application/json")
                        .content("{\"schedule_time\":\"08:00\"}"))
                .andExpect(status().isConflict());
    }
}