package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.CardInteraction;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.Patient;
import com.unify.onyak.domain.medications.service.EvidenceCardService;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvidenceCardController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class EvidenceCardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean EvidenceCardService evidenceCardService;

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        int status = mockMvc.perform(get("/wards/7/evidence-card")).andReturn().getResponse().getStatus();
        assertEquals(401, status);
        verifyNoInteractions(evidenceCardService);
    }

    @Test
    void 근거_카드를_조회한다() throws Exception {
        CustomUserDetails user = new CustomUserDetails(3L, "3", null, "ROLE_GUARDIAN");
        when(evidenceCardService.getEvidenceCard(any(), eq(7L))).thenReturn(new EvidenceCardResponse(
                7L, "김할머니", OffsetDateTime.parse("2026-10-13T18:00:00+09:00"), new Patient(78, false),
                "DANGER", "함께 먹으면 안 되는 조합이 1건 있습니다.", List.of(),
                List.of(new CardInteraction("병용금기", "CONTRAINDICATED", List.of(12L, 15L),
                        List.of("타이레놀정500mg", "테스트병용약정"), "간독성")),
                List.of(), "참고 정보"));

        mockMvc.perform(get("/wards/7/evidence-card")
                        .with(authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("DANGER"))
                .andExpect(jsonPath("$.data.patient.age").value(78))
                .andExpect(jsonPath("$.data.interactions[0].medication_ids[1]").value(15));
    }
}
