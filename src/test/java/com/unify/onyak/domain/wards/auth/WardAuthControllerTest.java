package com.unify.onyak.domain.wards.auth;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.wards.auth.controller.WardAuthController;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.domain.wards.auth.service.WardLoginService;
import com.unify.onyak.domain.wards.auth.service.WardSignUpService;
import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WardAuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class WardAuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WardSignUpService wardSignUpService;

    @MockitoBean
    WardLoginService wardLoginService;

    @MockitoBean
    JwtProvider jwtProvider;

    @Test
    void 미인증_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/ward/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(wardSignUpService);
    }

    @Test
    void 인증된_보호자는_피보호자를_가입시킬_수_있다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(1L, "guardian", null, "ROLE_GUARDIAN");
        when(wardSignUpService.signUp(any(), any()))
                .thenReturn(new WardSignUpResponse(
                        10L,
                        "피보호자",
                        new LinkCodeResponse(
                                "123456",
                                "qr-token",
                                OffsetDateTime.now().plusMinutes(5)
                        )
                ));

        mockMvc.perform(post("/ward/auth/signup")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ward_id").value(10))
                .andExpect(jsonPath("$.data.name").value("피보호자"))
                .andExpect(jsonPath("$.data.link_code_response.code").value("123456"))
                .andExpect(jsonPath("$.data.link_code_response.qr_token").value("qr-token"));

        verify(wardSignUpService).signUp(any(CustomUserDetails.class), any());
    }

    @Test
    void 필수값이_없는_요청은_400을_반환한다() throws Exception {
        CustomUserDetails guardian = new CustomUserDetails(1L, "guardian", null, "ROLE_GUARDIAN");

        mockMvc.perform(post("/ward/auth/signup")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                guardian, null, guardian.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(wardSignUpService);
    }

    @Test
    void 피보호자는_연동코드로_로그인할_수_있다() throws Exception {
        when(wardLoginService.login(any(WardLoginRequest.class), anyString()))
                .thenReturn(new LoginResponse(
                        10L,
                        null,
                        "피보호자",
                        Role.WARD,
                        "access-token",
                        "refresh-token"
                ));

        mockMvc.perform(post("/ward/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "123456",
                                  "qrToken": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("WARD"))
                .andExpect(jsonPath("$.data.access_token").value("access-token"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh-token"));

        var captor = forClass(WardLoginRequest.class);
        verify(wardLoginService).login(captor.capture(), anyString());
        assertEquals("123456", captor.getValue().code());
    }

    private String validJson() {
        return """
                {
                  "name": "피보호자",
                  "birth_date": "2010-01-01",
                  "weight": 50.0,
                  "height": 160.0,
                  "gender": "F",
                  "is_pregnant": false,
                  "medication_push_enabled": true,
                  "sos_push_enabled": true
                }
                """;
    }
}
