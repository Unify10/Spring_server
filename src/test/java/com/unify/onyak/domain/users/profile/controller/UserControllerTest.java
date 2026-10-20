package com.unify.onyak.domain.users.profile.controller;

import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;
import com.unify.onyak.domain.users.profile.service.SettingEditService;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean SettingEditService settingEditService;

    @Test
    void 인증_없이_환경설정을_변경하면_401이다() throws Exception {
        mockMvc.perform(patch("/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medication_push_enabled\":true}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(settingEditService);
    }

    @Test
    void 인증된_사용자는_snake_case_본문으로_환경설정을_변경한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(settingEditService.editSetting(eq(3L), any(SettingEditRequest.class)))
                .thenReturn(SettingEditResponse.builder()
                        .medicationPushEnabled(true)
                        .sosPushEnabled(false)
                        .marketingPushEnabled(false)
                        .build());

        mockMvc.perform(patch("/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medication_push_enabled\":true,\"sos_push_enabled\":false,\"marketing_push_enabled\":false}")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medication_push_enabled").value(true))
                .andExpect(jsonPath("$.data.sos_push_enabled").value(false))
                .andExpect(jsonPath("$.data.marketing_push_enabled").value(false));

        // 전역 SNAKE_CASE 가 본문 키를 camelCase 필드로 매핑했는지
        ArgumentCaptor<SettingEditRequest> captor = ArgumentCaptor.forClass(SettingEditRequest.class);
        verify(settingEditService).editSetting(eq(3L), captor.capture());
        assertThat(captor.getValue().medicationPushEnabled()).isTrue();
        assertThat(captor.getValue().sosPushEnabled()).isFalse();
        assertThat(captor.getValue().marketingPushEnabled()).isFalse();
    }
}
