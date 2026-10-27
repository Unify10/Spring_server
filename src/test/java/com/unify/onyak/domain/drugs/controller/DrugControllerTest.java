package com.unify.onyak.domain.drugs.controller;

import com.unify.onyak.domain.drugs.client.DrugProxyClient;
import com.unify.onyak.domain.drugs.error.exception.DrugApiUnavailableException;
import com.unify.onyak.domain.drugs.error.exception.DrugNotFoundException;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DrugController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class DrugControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean DrugProxyClient drugProxyClient;

    private static RequestPostProcessor ward() {
        CustomUserDetails user = new CustomUserDetails(5L, "5", null, "ROLE_WARD");
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        int status = mockMvc.perform(get("/drugs/search").param("q", "타이레놀")).andReturn().getResponse().getStatus();
        assertEquals(401, status);
        verifyNoInteractions(drugProxyClient);
    }

    @Test
    void 검색_결과를_FastAPI_응답_그대로_data에_담는다() throws Exception {
        when(drugProxyClient.search("타이레놀", 20, false)).thenReturn(JsonMapper.builder().build().readTree("""
                {"query":"타이레놀","total":1,"items":[{"drug_id":"195700020","item_name":"타이레놀정500mg"}]}
                """));

        // 피보호자도 약을 찾아볼 수 있다
        mockMvc.perform(get("/drugs/search").param("q", "타이레놀").with(ward()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].drug_id").value("195700020"));
    }

    @Test
    void 검색어가_한_글자면_400_VALIDATION_ERROR() throws Exception {
        mockMvc.perform(get("/drugs/search").param("q", "타").with(ward()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("q"));
        verifyNoInteractions(drugProxyClient);
    }

    @Test
    void 검색어가_없으면_400_MISSING_PARAMETER() throws Exception {
        mockMvc.perform(get("/drugs/search").with(ward()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_PARAMETER"));
    }

    @Test
    void 품목기준코드가_숫자가_아니면_FastAPI를_부르지_않고_400() throws Exception {
        mockMvc.perform(get("/drugs/abc").with(ward()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
        verifyNoInteractions(drugProxyClient);
    }

    @Test
    void 없는_약은_404_DRUG_NOT_FOUND() throws Exception {
        when(drugProxyClient.detail("000000000")).thenThrow(new DrugNotFoundException());

        mockMvc.perform(get("/drugs/000000000").with(ward()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("DRUG_NOT_FOUND"));
    }

    @Test
    void OCR_이미지를_넘기고_FastAPI가_죽어_있으면_502() throws Exception {
        when(drugProxyClient.ocr(any())).thenThrow(new DrugApiUnavailableException());

        mockMvc.perform(multipart("/drugs/ocr")
                        .file(new MockMultipartFile("file", "pill.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .with(ward()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error_code").value("DRUG_API_UNAVAILABLE"));
    }

    @Test
    void OCR에_파일이_없으면_400_MISSING_PARAMETER() throws Exception {
        mockMvc.perform(multipart("/drugs/ocr").with(ward()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("MISSING_PARAMETER"));
        verifyNoInteractions(drugProxyClient);
    }

    @Test
    void analyze는_나이_임신_여부를_넘긴다() throws Exception {
        when(drugProxyClient.analyze(any(), eq(78), eq(false)))
                .thenReturn(JsonMapper.builder().build().readTree("{\"classification\":\"일반의약품\"}"));

        mockMvc.perform(multipart("/drugs/analyze")
                        .file(new MockMultipartFile("file", "pill.jpg", "image/jpeg", new byte[]{1}))
                        .param("age", "78")
                        .param("pregnant", "false")
                        .with(ward()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classification").value("일반의약품"));
    }
}
