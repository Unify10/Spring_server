package com.unify.onyak.domain.wards.auth;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.service.WardLoginService;
import com.unify.onyak.domain.wards.auth.service.impl.WardLoginServiceImpl;
import com.unify.onyak.domain.wards.link_codes.service.VerifyLinkCodeService;
import com.unify.onyak.global.jwt.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WardLoginServiceTest {

    private VerifyLinkCodeService verifyLinkCodeService;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtProvider jwtProvider;
    private WardLoginService wardLoginService;

    @BeforeEach
    void setUp() {
        verifyLinkCodeService = mock(VerifyLinkCodeService.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        jwtProvider = mock(JwtProvider.class);
        wardLoginService = new WardLoginServiceImpl(
                verifyLinkCodeService,
                refreshTokenRepository,
                jwtProvider
        );
    }

    @Test
    void 연동코드_검증_후_피보호자용_토큰을_발급한다() {
        Ward ward = mock(Ward.class);
        when(ward.getId()).thenReturn(7L);
        when(verifyLinkCodeService.verifyLinkCode(
                any(WardLoginRequest.class))).thenReturn(ward);
        when(jwtProvider.generateAccessToken(7L, Role.WARD))
                .thenReturn("ward-access-token");
        when(jwtProvider.generateRefreshToken(7L, Role.WARD))
                .thenReturn("ward-refresh-token");
        when(jwtProvider.getRefreshTokenExpTime()).thenReturn(1_000L);

        LoginResponse response = wardLoginService.login(
                new WardLoginRequest("123456", null)
        );

        assertEquals("ward-access-token", response.accessToken());
        assertEquals("ward-refresh-token", response.refreshToken());
        verify(verifyLinkCodeService).verifyLinkCode(any(WardLoginRequest.class));
        verify(jwtProvider).generateAccessToken(7L, Role.WARD);
        verify(jwtProvider).generateRefreshToken(7L, Role.WARD);

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(7L, saved.getUserId());
        assertEquals(Role.WARD, saved.getRole());
        assertEquals("ward-refresh-token", saved.getRefreshToken());
        assertEquals(1_000L, saved.getTtl());
    }
}
