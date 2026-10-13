package com.unify.onyak.global.jwt;

import com.unify.onyak.domain.users.auth.controller.AuthController;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.users.auth.service.EmailVerificationService;
import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationEntryPoint.class})
class JwtAuthenticationFilterTest {

    private static final String BEARER = "Bearer ";

    @Autowired MockMvc mockMvc;
    @Autowired JwtProvider jwtProvider;
    @Value("${jwt.secret}") String secret;
    @MockitoBean AuthService authService;
    @MockitoBean UserSignUpService userSignUpService;
    @MockitoBean EmailVerificationService emailVerificationService;

    @Test
    void 유효한_access_token이면_인증되어_통과한다() throws Exception {
        String token = jwtProvider.generateAccessToken(7L, Role.WARD);

        mockMvc.perform(post("/auth/logout").header("Authorization", BEARER + token))
                .andExpect(status().isNoContent());

        verify(authService).logout(7L);
    }

    @Test
    void 토큰이_없으면_보호_경로는_401이다() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("AUTH_UNAUTHORIZED"));

        verifyNoInteractions(authService);
    }

    @Test
    void 만료된_토큰이면_401_AUTH_TOKEN_EXPIRED이다() throws Exception {
        String token = new JwtProvider(secret, -1000L, -1000L).generateAccessToken(7L, Role.WARD);

        mockMvc.perform(post("/auth/logout").header("Authorization", BEARER + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("AUTH_TOKEN_EXPIRED"));

        verifyNoInteractions(authService);
    }

    @Test
    void 위조된_토큰이면_401_AUTH_INVALID_TOKEN이다() throws Exception {
        String token = jwtProvider.generateAccessToken(7L, Role.WARD) + "tampered";

        mockMvc.perform(post("/auth/logout").header("Authorization", BEARER + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("AUTH_INVALID_TOKEN"));

        verifyNoInteractions(authService);
    }

    @Test
    void refresh_token을_access_자리에_쓰면_401이다() throws Exception {
        String token = jwtProvider.generateRefreshToken(7L, Role.WARD);

        mockMvc.perform(post("/auth/logout").header("Authorization", BEARER + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("AUTH_INVALID_TOKEN"));

        verifyNoInteractions(authService);
    }

    @Test
    void 만료된_토큰이_있어도_공개_경로는_401이_아니다() throws Exception {
        String token = new JwtProvider(secret, -1000L, -1000L).generateAccessToken(7L, Role.WARD);

        int status = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", BEARER + token))
                .andReturn().getResponse().getStatus();

        assertNotEquals(401, status);
    }
}
