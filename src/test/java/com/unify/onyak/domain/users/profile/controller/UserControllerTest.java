package com.unify.onyak.domain.users.profile.controller;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.profile.dto.request.BodyEditRequest;
import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.BodyEditResponse;
import com.unify.onyak.domain.users.profile.dto.response.MyProfileResponse;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;
import com.unify.onyak.domain.users.profile.error.exception.InvalidPregnancy;
import com.unify.onyak.domain.users.profile.error.exception.UserForbidden;
import com.unify.onyak.domain.users.profile.service.BodyEditService;
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

import java.time.LocalDate;

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
    @MockitoBean BodyEditService bodyEditService;

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
        when(settingEditService.editSetting(eq(3L), eq(Role.GUARDIAN), any(SettingEditRequest.class)))
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
        verify(settingEditService).editSetting(eq(3L), eq(Role.GUARDIAN), captor.capture());
        assertThat(captor.getValue().medicationPushEnabled()).isTrue();
        assertThat(captor.getValue().sosPushEnabled()).isFalse();
        assertThat(captor.getValue().marketingPushEnabled()).isFalse();
    }

    @Test
    void 인증된_사용자는_자신의_프로필을_snake_case_본문으로_조회한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(myProfileService.getMyProfile(3L, Role.GUARDIAN))
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
        verify(myProfileService).getMyProfile(3L,  Role.GUARDIAN);
    }

    @Test
    void 피보호자는_환경설정을_변경할_수_없다() throws Exception {
        CustomUserDetails ward = new CustomUserDetails(3L, "3", null, "ROLE_WARD");
        when(settingEditService.editSetting(
                eq(3L), eq(Role.WARD), any(SettingEditRequest.class)))
                .thenThrow(new UserForbidden());

        mockMvc.perform(patch("/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medication_push_enabled\":true}")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                ward, null, ward.getAuthorities()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void 인증이_없으면_401을_반환하고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(myProfileService);
    }

    @Test
    void 보호자가_아닌_사용자가_내정보조회_하면_403을_반환한다() throws Exception {
        CustomUserDetails ward = new CustomUserDetails(3L, "a@a.com", null, "ROLE_WARD");
        when(myProfileService.getMyProfile(3L, Role.WARD))
                .thenThrow(new UserForbidden());

        mockMvc.perform(get("/users/me")
                .with(authentication(new UsernamePasswordAuthenticationToken(
                        ward, null, ward.getAuthorities()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void 인증_없이_신체정보를_수정하면_401이다() throws Exception {
        mockMvc.perform(patch("/users/me/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":60.5}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bodyEditService);
    }

    @Test
    void 보호자는_snake_case_본문으로_신체정보를_수정한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(bodyEditService.editBody(eq(3L), eq(Role.GUARDIAN), any(BodyEditRequest.class)))
                .thenReturn(new BodyEditResponse(
                        LocalDate.of(1990, 5, 1), 55.5f, 162.0f, Gender.F, true));

        mockMvc.perform(patch("/users/me/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "birth_date": "1990-05-01",
                                  "weight": 55.5,
                                  "height": 162.0,
                                  "gender": "F",
                                  "is_pregnant": true
                                }
                                """)
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.birth_date").value("1990-05-01"))
                .andExpect(jsonPath("$.data.weight").value(55.5))
                .andExpect(jsonPath("$.data.height").value(162.0))
                .andExpect(jsonPath("$.data.gender").value("F"))
                .andExpect(jsonPath("$.data.is_pregnant").value(true));

        // 전역 SNAKE_CASE 가 본문 키를 camelCase 필드로 매핑했는지
        ArgumentCaptor<BodyEditRequest> captor = ArgumentCaptor.forClass(BodyEditRequest.class);
        verify(bodyEditService).editBody(eq(3L), eq(Role.GUARDIAN), captor.capture());
        assertThat(captor.getValue().birthDate()).isEqualTo(LocalDate.of(1990, 5, 1));
        assertThat(captor.getValue().weight()).isEqualTo(55.5f);
        assertThat(captor.getValue().height()).isEqualTo(162.0f);
        assertThat(captor.getValue().gender()).isEqualTo(Gender.F);
        assertThat(captor.getValue().isPregnant()).isTrue();
    }

    @Test
    void 피보호자는_신체정보를_수정할_수_없다() throws Exception {
        CustomUserDetails ward = new CustomUserDetails(3L, "3", null, "ROLE_WARD");
        when(bodyEditService.editBody(eq(3L), eq(Role.WARD), any(BodyEditRequest.class)))
                .thenThrow(new UserForbidden());

        mockMvc.perform(patch("/users/me/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":60.5}")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                ward, null, ward.getAuthorities()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void 남성이_임신으로_수정하면_400이다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");
        when(bodyEditService.editBody(eq(3L), eq(Role.GUARDIAN), any(BodyEditRequest.class)))
                .thenThrow(new InvalidPregnancy());

        mockMvc.perform(patch("/users/me/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"M\",\"is_pregnant\":true}")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 몸무게가_음수면_400이고_서비스를_호출하지_않는다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");

        mockMvc.perform(patch("/users/me/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":-1}")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bodyEditService);
    }

    @Test
    void 생년월일이_미래면_400이고_서비스를_호출하지_않는다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(3L, "a@a.com", null, "ROLE_GUARDIAN");

        mockMvc.perform(patch("/users/me/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"birth_date\":\"" + LocalDate.now().plusDays(1) + "\"}")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities()))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bodyEditService);
    }
}
