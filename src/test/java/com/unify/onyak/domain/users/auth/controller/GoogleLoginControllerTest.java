package com.unify.onyak.domain.users.auth.controller;

import com.unify.onyak.domain.users.auth.dto.request.GoogleLoginRequest;
import com.unify.onyak.domain.users.auth.dto.response.GoogleLoginResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.error.exception.GoogleTokenInvalidException;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.users.auth.service.EmailVerificationService;
import com.unify.onyak.domain.users.auth.service.GoogleLoginService;
import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class GoogleLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserSignUpService userSignUpService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private GoogleLoginService googleLoginService;

    @Test
    @DisplayName("신규 구글 가입 시 201 Created 반환 및 snake_case 응답 키 검증 (is_new_user=true, role=GUARDIAN)")
    void 신규_구글_가입_성공_201() throws Exception {
        GoogleLoginResponse response = new GoogleLoginResponse(
                1L,
                "newuser@example.com",
                "신규유저",
                Role.GUARDIAN,
                "access-token-val",
                "refresh-token-val",
                true
        );
        when(googleLoginService.login(any(GoogleLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_token\":\"valid-id-token\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user_id").value(1))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.name").value("신규유저"))
                .andExpect(jsonPath("$.data.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.data.access_token").value("access-token-val"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh-token-val"))
                .andExpect(jsonPath("$.data.is_new_user").value(true));
    }

    @Test
    @DisplayName("기존 구글 로그인 시 200 OK 반환 (is_new_user=false, role=GUARDIAN)")
    void 기존_구글_로그인_성공_200() throws Exception {
        GoogleLoginResponse response = new GoogleLoginResponse(
                2L,
                "existing@example.com",
                "기존유저",
                Role.GUARDIAN,
                "access-token-val",
                "refresh-token-val",
                false
        );
        when(googleLoginService.login(any(GoogleLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_token\":\"valid-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user_id").value(2))
                .andExpect(jsonPath("$.data.email").value("existing@example.com"))
                .andExpect(jsonPath("$.data.name").value("기존유저"))
                .andExpect(jsonPath("$.data.role").value("GUARDIAN"))
                .andExpect(jsonPath("$.data.access_token").value("access-token-val"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh-token-val"))
                .andExpect(jsonPath("$.data.is_new_user").value(false));
    }

    @Test
    @DisplayName("id_token 누락 시 400 Bad Request")
    void id_token_누락_400() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_token\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("id_token이 4096자를 초과하면 400 Bad Request (VALIDATION_ERROR)")
    void id_token_길이초과_400() throws Exception {
        String hugeToken = "a".repeat(4097);
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_token\":\"" + hugeToken + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("검증 실패 시 400 Bad Request 및 AUTH_GOOGLE_TOKEN_INVALID 에러 코드 반환")
    void 검증_실패_400() throws Exception {
        when(googleLoginService.login(any(GoogleLoginRequest.class)))
                .thenThrow(new GoogleTokenInvalidException());

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_token\":\"invalid-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("AUTH_GOOGLE_TOKEN_INVALID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 구글 토큰입니다."));
    }
}
