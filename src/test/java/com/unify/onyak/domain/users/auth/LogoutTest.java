package com.unify.onyak.domain.users.auth;

import com.unify.onyak.domain.users.auth.controller.AuthController;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.users.auth.service.EmailVerificationService;
import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.service.impl.AuthServiceImpl;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtProvider;
import com.unify.onyak.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class LogoutTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;
    @MockitoBean UserSignUpService userSignUpService;
    @MockitoBean EmailVerificationService emailVerificationService;

    @Test
    void 미인증_요청은_거부된다() throws Exception {
        int s = mockMvc.perform(post("/auth/logout")).andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(s == 401 || s == 403, "status=" + s);
        verifyNoInteractions(authService);
    }

    @Test
    void 인증된_피보호자는_WARD_역할로_로그아웃된다() throws Exception {
        CustomUserDetails ward = new CustomUserDetails(7L, "code", null, "ROLE_WARD");
        mockMvc.perform(post("/auth/logout")
                        .with(authentication(new UsernamePasswordAuthenticationToken(ward, null, ward.getAuthorities()))))
                .andExpect(status().isNoContent());
        verify(authService).logout(7L);
    }

    @Test
    void 서비스는_사용자의_refresh_token만_삭제한다() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        UserValidationPolicy userValidationPolicy = mock(UserValidationPolicy.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);

        new AuthServiceImpl(jwtProvider, userValidationPolicy, refreshTokenRepository).logout(3L);

        verify(refreshTokenRepository).deleteById(3L);
    }
}
