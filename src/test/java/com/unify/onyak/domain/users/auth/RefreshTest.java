package com.unify.onyak.domain.users.auth;

import com.unify.onyak.domain.users.auth.controller.AuthController;
import com.unify.onyak.domain.users.auth.dto.request.RefreshRequest;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.error.exception.InvalidTokenException;
import com.unify.onyak.domain.users.auth.error.exception.TokenExpiredException;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.users.auth.service.EmailVerificationService;
import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.domain.users.auth.service.impl.AuthServiceImpl;
import com.unify.onyak.global.config.SecurityConfig;
import com.unify.onyak.global.jwt.JwtAuthenticationEntryPoint;
import com.unify.onyak.global.jwt.JwtProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class RefreshTest {

    private static final String SECRET = "dGVzdC1vbmx5LXNlY3JldC1rZXktbm90LWZvci1wcm9kdWN0aW9uLXVzZS0xMjM0NTY3OA==";
    private static final long ACCESS_EXP = 3_600_000L;
    private static final long REFRESH_EXP = 7_200_000L;

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtProvider jwtProviderBean;
    @MockitoBean AuthService authService;
    @MockitoBean UserSignUpService userSignUpService;
    @MockitoBean EmailVerificationService emailVerificationService;

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, ACCESS_EXP, REFRESH_EXP);
    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final AuthServiceImpl service = new AuthServiceImpl(
            jwtProvider, mock(UserValidationPolicy.class), repository);

    @Test
    void 컨트롤러는_공개경로로_토큰을_재발급해_응답한다() throws Exception {
        when(authService.refresh(any())).thenReturn(new TokenResponse("new-access", "new-refresh"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"old\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value("new-access"))
                .andExpect(jsonPath("$.data.refresh_token").value("new-refresh"))
                // refresh 응답은 토큰 2개만 (user_id·email·name·role 없음)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data.user_id").doesNotExist());
    }

    @Test
    void refresh_token이_비어있으면_400이다() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 저장된_토큰과_일치하면_새_토큰을_발급하고_Redis_값을_교체한다() {
        String old = jwtProvider.generateRefreshToken(7L, Role.WARD);
        when(repository.findById("WARD:7")).thenReturn(Optional.of(new RefreshToken(7L, Role.WARD, old, REFRESH_EXP)));

        TokenResponse response = service.refresh(new RefreshRequest(old));

        assertEquals(7L, jwtProvider.getUserId(response.accessToken()));
        assertEquals(Role.WARD, jwtProvider.getRole(response.refreshToken()));
        assertNotEquals(old, response.refreshToken());
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void 같은_초에_연속_발급해도_토큰이_서로_다르다() {
        assertNotEquals(
                jwtProvider.generateRefreshToken(7L, Role.WARD),
                jwtProvider.generateRefreshToken(7L, Role.WARD));
    }

    @Test
    void Redis에_없으면_AUTH_INVALID_TOKEN이다() {
        String token = jwtProvider.generateRefreshToken(7L, Role.WARD);
        when(repository.findById("WARD:7")).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> service.refresh(new RefreshRequest(token)));
        verify(repository, never()).save(any());
    }

    @Test
    void 저장된_값과_다르면_AUTH_INVALID_TOKEN이다() {
        String old = jwtProvider.generateRefreshToken(7L, Role.WARD);
        String rotated = jwtProvider.generateRefreshToken(7L, Role.WARD);
        when(repository.findById("WARD:7")).thenReturn(Optional.of(new RefreshToken(7L, Role.WARD, rotated, REFRESH_EXP)));

        assertThrows(InvalidTokenException.class, () -> service.refresh(new RefreshRequest(old)));
        verify(repository, never()).save(any());
    }

    @Test
    void access_token을_보내면_AUTH_INVALID_TOKEN이다() {
        String access = jwtProvider.generateAccessToken(7L, Role.WARD);

        assertThrows(InvalidTokenException.class, () -> service.refresh(new RefreshRequest(access)));
    }

    @Test
    void 만료된_refresh_token이면_AUTH_TOKEN_EXPIRED이다() {
        String expired = new JwtProvider(SECRET, ACCESS_EXP, -1000L).generateRefreshToken(7L, Role.WARD);

        assertThrows(TokenExpiredException.class, () -> service.refresh(new RefreshRequest(expired)));
    }

    @Test
    void 위조되거나_role이_이상한_토큰이면_AUTH_INVALID_TOKEN이다() {
        String tampered = jwtProvider.generateRefreshToken(7L, Role.WARD) + "x";
        String badRole = Jwts.builder()
                .subject("7")
                .claim("type", "refresh")
                .claim("role", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(InvalidTokenException.class, () -> service.refresh(new RefreshRequest(tampered)));
        assertThrows(InvalidTokenException.class, () -> service.refresh(new RefreshRequest(badRole)));
    }
}
