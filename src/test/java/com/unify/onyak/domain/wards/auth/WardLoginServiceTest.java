package com.unify.onyak.domain.wards.auth;

import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.wards.auth.dto.request.WardLoginRequest;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.error.exception.TooManyLoginRequestException;
import com.unify.onyak.domain.wards.auth.service.WardLoginAttemptService;
import com.unify.onyak.domain.wards.auth.service.WardLoginService;
import com.unify.onyak.domain.wards.auth.service.impl.WardLoginServiceImpl;
import com.unify.onyak.domain.wards.link_codes.error.exception.InvalidLinkCodeException;
import com.unify.onyak.domain.wards.link_codes.service.VerifyLinkCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WardLoginServiceTest {

    private VerifyLinkCodeService verifyLinkCodeService;
    private AuthService authService;
    private WardLoginAttemptService wardLoginAttemptService;
    private WardLoginService wardLoginService;

    @BeforeEach
    void setUp() {
        verifyLinkCodeService = mock(VerifyLinkCodeService.class);
        authService = mock(AuthService.class);
        wardLoginAttemptService = mock(WardLoginAttemptService.class);
        wardLoginService = new WardLoginServiceImpl(
                verifyLinkCodeService,
                authService,
                wardLoginAttemptService
        );
    }

    @Test
    void 연동코드_검증_후_피보호자용_토큰을_발급한다() {
        Ward ward = mock(Ward.class);
        when(ward.getId()).thenReturn(7L);
        when(ward.getName()).thenReturn("피보호자");
        when(verifyLinkCodeService.verifyLinkCode(
                any(WardLoginRequest.class))).thenReturn(ward);
        when(authService.issueTokens(7L, Role.WARD))
                .thenReturn(new TokenResponse("ward-access-token", "ward-refresh-token"));

        LoginResponse response = wardLoginService.login(
                new WardLoginRequest("123456", null),
                "127.0.0.1"
        );

        assertEquals("ward-access-token", response.accessToken());
        assertEquals("ward-refresh-token", response.refreshToken());
        verify(verifyLinkCodeService).verifyLinkCode(any(WardLoginRequest.class));
        verify(authService).issueTokens(7L, Role.WARD);
        assertEquals(7L, response.userId());
        assertEquals("피보호자", response.name());
        assertEquals(Role.WARD, response.role());
    }

    @Test
    void 연동코드가_잘못되면_IP별_실패횟수를_증가시키고_예외를_던진다() {
        when(verifyLinkCodeService.verifyLinkCode(any(WardLoginRequest.class)))
                .thenThrow(new InvalidLinkCodeException());

        assertThrows(
                InvalidLinkCodeException.class,
                () -> wardLoginService.login(
                        new WardLoginRequest("999999", null),
                        "127.0.0.1"
                )
        );

        verify(wardLoginAttemptService).recordFailure("127.0.0.1");
        verifyNoInteractions(authService);
    }

    @Test
    void IP가_차단된_경우_연동코드_검증을_수행하지_않고_예외를_던진다() {
        when(wardLoginAttemptService.isBlocked("127.0.0.1")).thenReturn(true);

        assertThrows(
                TooManyLoginRequestException.class,
                () -> wardLoginService.login(
                        new WardLoginRequest("123456", null),
                        "127.0.0.1"
                )
        );

        verifyNoInteractions(verifyLinkCodeService, authService);
    }
}
