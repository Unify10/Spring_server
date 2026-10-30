package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.GoogleUserInfo;
import com.unify.onyak.domain.users.auth.dto.request.GoogleLoginRequest;
import com.unify.onyak.domain.users.auth.dto.response.GoogleLoginResponse;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.DuplicateEmailException;
import com.unify.onyak.domain.users.auth.error.exception.GoogleTokenInvalidException;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.users.auth.service.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleLoginServiceImplTest {

    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private GoogleLoginServiceImpl googleLoginService;

    private static final String ID_TOKEN = "valid.google.id.token";
    private static final String GOOGLE_ID = "google-12345";
    private static final String EMAIL = "test@example.com";
    private static final String NAME = "홍길동";

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("신규 가입: phone은 NULL, email_verified는 true, google_id 저장, GUARDIAN 토큰 발급, is_new_user=true")
    void 신규가입_성공() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest(ID_TOKEN);
        GoogleUserInfo userInfo = new GoogleUserInfo(GOOGLE_ID, EMAIL, NAME);
        when(googleIdTokenVerifier.verify(ID_TOKEN)).thenReturn(userInfo);
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 100L);
            return u;
        });
        when(authService.issueTokens(100L, Role.GUARDIAN))
                .thenReturn(new TokenResponse("access-token-123", "refresh-token-123"));

        // when
        GoogleLoginResponse response = googleLoginService.login(request);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getName()).isEqualTo(NAME);
        assertThat(savedUser.getGoogleId()).isEqualTo(GOOGLE_ID);
        assertThat(savedUser.getPasswordHash()).isNull();
        assertThat(savedUser.getPhone()).isNull();
        assertThat(savedUser.getEmailVerified()).isTrue();
        assertThat(savedUser.getMedicationPushEnabled()).isTrue();
        assertThat(savedUser.getSosPushEnabled()).isTrue();
        assertThat(savedUser.getMarketingPushEnabled()).isTrue();

        verify(authService).issueTokens(100L, Role.GUARDIAN);
        assertThat(response.userId()).isEqualTo(100L);
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.name()).isEqualTo(NAME);
        assertThat(response.role()).isEqualTo(Role.GUARDIAN);
        assertThat(response.accessToken()).isEqualTo("access-token-123");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-123");
        assertThat(response.isNewUser()).isTrue();
    }

    @Test
    @DisplayName("기존 google_id 사용자 로그인: issueTokens 호출, is_new_user=false")
    void 기존_google_id_로그인_성공() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest(ID_TOKEN);
        GoogleUserInfo userInfo = new GoogleUserInfo(GOOGLE_ID, EMAIL, NAME);
        when(googleIdTokenVerifier.verify(ID_TOKEN)).thenReturn(userInfo);

        User existingUser = User.createGoogle(EMAIL, NAME, GOOGLE_ID);
        ReflectionTestUtils.setField(existingUser, "id", 200L);
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.of(existingUser));

        when(authService.issueTokens(200L, Role.GUARDIAN))
                .thenReturn(new TokenResponse("access-token-200", "refresh-token-200"));

        // when
        GoogleLoginResponse response = googleLoginService.login(request);

        // then
        verify(userRepository, never()).save(any(User.class));
        verify(authService).issueTokens(200L, Role.GUARDIAN);
        assertThat(response.userId()).isEqualTo(200L);
        assertThat(response.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("같은 이메일의 기존 계정 연동 (email_verified=true): google_id 연결 후 로그인, is_new_user=false")
    void 기존계정_이메일인증완료_연동_성공() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest(ID_TOKEN);
        GoogleUserInfo userInfo = new GoogleUserInfo(GOOGLE_ID, EMAIL, NAME);
        when(googleIdTokenVerifier.verify(ID_TOKEN)).thenReturn(userInfo);
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());

        User existingUser = User.createLocal(EMAIL, "existing-hash", NAME, "010-1234-5678", null, null, null, null, null, true, true, true);
        ReflectionTestUtils.setField(existingUser, "id", 300L);
        ReflectionTestUtils.setField(existingUser, "emailVerified", true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        when(authService.issueTokens(300L, Role.GUARDIAN))
                .thenReturn(new TokenResponse("access-token-300", "refresh-token-300"));

        // when
        GoogleLoginResponse response = googleLoginService.login(request);

        // then
        assertThat(existingUser.getGoogleId()).isEqualTo(GOOGLE_ID);
        assertThat(existingUser.getPasswordHash()).isEqualTo("existing-hash"); // 기존 비밀번호 유지
        verify(userRepository).saveAndFlush(existingUser);
        verify(authService).issueTokens(300L, Role.GUARDIAN);
        assertThat(response.userId()).isEqualTo(300L);
        assertThat(response.isNewUser()).isFalse();
    }

    @Test
    @DisplayName("같은 이메일이지만 email_verified=false인 경우 연동 거부 -> 409 DuplicateEmailException")
    void 기존계정_이메일미인증_연동_실패() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest(ID_TOKEN);
        GoogleUserInfo userInfo = new GoogleUserInfo(GOOGLE_ID, EMAIL, NAME);
        when(googleIdTokenVerifier.verify(ID_TOKEN)).thenReturn(userInfo);
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());

        User existingUser = User.createLocal(EMAIL, "existing-hash", NAME, "010-1234-5678", null, null, null, null, null, true, true, true);
        ReflectionTestUtils.setField(existingUser, "id", 400L);
        ReflectionTestUtils.setField(existingUser, "emailVerified", false);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> googleLoginService.login(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(authService, never()).issueTokens(any(), any());
    }

    @Test
    @DisplayName("토큰 검증 실패 시 AUTH_GOOGLE_TOKEN_INVALID(GoogleTokenInvalidException) 발생")
    void 토큰검증_실패() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest("invalid-token");
        when(googleIdTokenVerifier.verify("invalid-token")).thenThrow(new GoogleTokenInvalidException());

        // when & then
        assertThatThrownBy(() -> googleLoginService.login(request))
                .isInstanceOf(GoogleTokenInvalidException.class);

        verify(userRepository, never()).findByGoogleId(any());
        verify(userRepository, never()).findByEmail(any());
        verify(authService, never()).issueTokens(any(), any());
    }

    @Test
    @DisplayName("동시 요청 신규가입 시 UNIQUE 충돌(DataIntegrityViolationException) 발생하면 409 DuplicateEmailException 변환 및 Redis 토큰 발급 방지")
    void 동시요청_신규가입_UNIQUE충돌_방어() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest(ID_TOKEN);
        GoogleUserInfo userInfo = new GoogleUserInfo(GOOGLE_ID, EMAIL, NAME);
        when(googleIdTokenVerifier.verify(ID_TOKEN)).thenReturn(userInfo);
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        // when & then
        assertThatThrownBy(() -> googleLoginService.login(request))
                .isInstanceOf(DuplicateEmailException.class);

        // Redis 토큰 발급이 실행되지 않아야 함 (고아 refresh 토큰 방지)
        verify(authService, never()).issueTokens(any(), any());
    }

    @Test
    @DisplayName("동시 요청 계정연동 시 UNIQUE 충돌(DataIntegrityViolationException) 발생하면 409 DuplicateEmailException 변환 및 Redis 토큰 발급 방지")
    void 동시요청_계정연동_UNIQUE충돌_방어() {
        // given
        GoogleLoginRequest request = new GoogleLoginRequest(ID_TOKEN);
        GoogleUserInfo userInfo = new GoogleUserInfo(GOOGLE_ID, EMAIL, NAME);
        when(googleIdTokenVerifier.verify(ID_TOKEN)).thenReturn(userInfo);
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());

        User existingUser = User.createLocal(EMAIL, "existing-hash", NAME, "010-1234-5678", null, null, null, null, null, true, true, true);
        ReflectionTestUtils.setField(existingUser, "id", 500L);
        ReflectionTestUtils.setField(existingUser, "emailVerified", true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        when(userRepository.saveAndFlush(existingUser))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        // when & then
        assertThatThrownBy(() -> googleLoginService.login(request))
                .isInstanceOf(DuplicateEmailException.class);

        // Redis 토큰 발급이 실행되지 않아야 함
        verify(authService, never()).issueTokens(any(), any());
    }
}
