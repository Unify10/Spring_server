package com.unify.onyak.domain.users.profile.controller;

import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.MyProfileResponse;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;
import com.unify.onyak.domain.users.profile.service.MyProfileService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean SettingEditService settingEditService;
    @MockitoBean private MyProfileService myProfileService;

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

    @Test
    void 인증된_사용자는_자신의_프로필을_snake_case_본문으로_조회한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(myProfileService.getMyProfile(3L))
                .thenReturn(MyProfileResponse.builder()
                        .userId(3L)
                        .email("a@a.com")
                        .name("테스트")
                        .phone("010-0000-0000")
                        .emailVerified(true)
                        .age(25)
                        .weight(60.5f)
                        .gender("F")
                        .isPregnant(false)
                        .build());

        mockMvc.perform(get("/users/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(3))
                .andExpect(jsonPath("$.data.email").value("a@a.com"))
                .andExpect(jsonPath("$.data.name").value("테스트"))
                .andExpect(jsonPath("$.data.phone").value("010-0000-0000"))
                .andExpect(jsonPath("$.data.email_verified").value(true))
                .andExpect(jsonPath("$.data.age").value(25))
                .andExpect(jsonPath("$.data.weight").value(60.5))
                .andExpect(jsonPath("$.data.gender").value("F"))
                .andExpect(jsonPath("$.data.is_pregnant").value(false));

        // 서비스에 넘기는 userId가 요청이 아니라 인증 principal에서 온 값인지
        verify(myProfileService).getMyProfile(3L);
    }

    @Test
    void 인증이_없으면_401을_반환하고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(myProfileService);
    }
}
