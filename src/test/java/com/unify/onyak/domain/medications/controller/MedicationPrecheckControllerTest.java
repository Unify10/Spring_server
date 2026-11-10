package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.request.MedicationPrecheckRequest;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.CardInteraction;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.Patient;
import com.unify.onyak.domain.medications.dto.response.MedicationPrecheckResponse;
import com.unify.onyak.domain.medications.dto.response.MedicationPrecheckResponse.PrecheckDrug;
import com.unify.onyak.domain.medications.service.MedicationPrecheckService;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicationPrecheckController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class MedicationPrecheckControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean MedicationPrecheckService medicationPrecheckService;

    static RequestPostProcessor guardian() {
        CustomUserDetails user = new CustomUserDetails(3L, "3", null, "ROLE_GUARDIAN");
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void 미인증_요청은_401() throws Exception {
        int status = mockMvc.perform(post("/wards/7/medications/precheck")
                .contentType("application/json").content("{\"item_seq\":\"195700020\"}"))
                .andReturn().getResponse().getStatus();
        assertEquals(401, status);
        verifyNoInteractions(medicationPrecheckService);
    }

    @Test
    void 담기_전_점검_결과를_돌려준다() throws Exception {
        when(medicationPrecheckService.precheck(any(), eq(7L), any(MedicationPrecheckRequest.class)))
                .thenReturn(new MedicationPrecheckResponse(7L, new Patient(78, false), "DANGER",
                        "복용 중인 테스트병용약정와(과) 함께 먹으면 안 됩니다.", false,
                        new PrecheckDrug("195700020", "타이레놀정500mg", "일반의약품", 0.86,
                                List.of(), List.of(), Map.of(), false),
                        List.of(new CardInteraction("병용금기", "CONTRAINDICATED", List.of(15L),
                                List.of("타이레놀정500mg", "테스트병용약정"), "간독성")),
                        List.of(), "참고 정보"));

        mockMvc.perform(post("/wards/7/medications/precheck").with(guardian())
                        .contentType("application/json").content("{\"item_seq\":\"195700020\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("DANGER"))
                .andExpect(jsonPath("$.data.drug.drug_id").value("195700020"))
                .andExpect(jsonPath("$.data.interactions[0].medication_ids[0]").value(15))
                .andExpect(jsonPath("$.data.already_taking").value(false));
    }

    @Test
    void item_seq가_없으면_400() throws Exception {
        mockMvc.perform(post("/wards/7/medications/precheck").with(guardian())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(medicationPrecheckService);
    }
}
