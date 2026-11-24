package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.request.PasswordResetRequest;
import com.unify.onyak.domain.users.auth.entity.PasswordReset;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.EmailCodeAttemptsExceededException;
import com.unify.onyak.domain.users.auth.error.exception.EmailCodeMismatchException;
import com.unify.onyak.domain.users.auth.error.exception.EmailNotVerifiedException;
import com.unify.onyak.domain.users.auth.error.exception.PasswordMismatchException;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.PasswordResetRepository;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private static final String EMAIL = "test@example.com";
    private static final String CODE = "K617WW";
    private static final String NEW_PASSWORD = "newPassword1234";

    @Mock private PasswordResetRepository passwordResetRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender emailSender;
    @Mock private UserValidationPolicy userValidationPolicy;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User localUser() {
        User user = User.createLocal(EMAIL, "oldHash", "홍길동", "010-1234-5678",
                null, null, null, null, null, true, true, true);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    // ---------- 1단계: 발송 ----------

    @Test
    @DisplayName("이메일로 가입한 사용자면 코드 메일을 보내고 새 코드를 저장한다")
    void 가입된_이메일이면_코드를_발송하고_저장한다() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(localUser()));
        when(emailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        passwordResetService.sendCode(EMAIL);

        verify(emailSender).send(any(MimeMessage.class));
        ArgumentCaptor<PasswordReset> captor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(captor.getValue().getCode()).matches("^[0-9A-Z]{6}$");
        assertThat(captor.getValue().isVerified()).isFalse();
        assertThat(captor.getValue().getFailedAttempts()).isZero();
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 예외 없이 끝나고 메일을 보내지 않는다")
    void 없는_이메일이면_조용히_끝낸다() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        passwordResetService.sendCode(EMAIL);

        verify(emailSender, never()).send(any(MimeMessage.class));
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("구글로만 가입한 계정(비밀번호 없음)이면 메일을 보내지 않는다")
    void 구글_계정이면_발송하지_않는다() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(User.createGoogle(EMAIL, "홍길동", "google-id")));

        passwordResetService.sendCode(EMAIL);

        verify(emailSender, never()).send(any(MimeMessage.class));
        verify(passwordResetRepository, never()).save(any());
    }

    // ---------- 2단계: 코드 확인 ----------

    @Test
    @DisplayName("코드가 맞으면 인증 완료로 저장한다")
    void 코드가_맞으면_인증_완료로_저장한다() {
        when(passwordResetRepository.findById(EMAIL))
                .thenReturn(Optional.of(PasswordReset.issue(EMAIL, CODE)));

        passwordResetService.verifyCode(EMAIL, CODE);

        ArgumentCaptor<PasswordReset> captor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).save(captor.capture());
        assertThat(captor.getValue().isVerified()).isTrue();
    }

    @Test
    @DisplayName("코드가 틀리면 실패 횟수를 1 올려 저장하고 예외를 던진다")
    void 코드가_틀리면_실패_횟수를_올린다() {
        when(passwordResetRepository.findById(EMAIL))
                .thenReturn(Optional.of(PasswordReset.issue(EMAIL, CODE)));

        assertThatThrownBy(() -> passwordResetService.verifyCode(EMAIL, "BWJFAS"))
                .isInstanceOf(EmailCodeMismatchException.class);

        ArgumentCaptor<PasswordReset> captor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedAttempts()).isEqualTo(1);
        assertThat(captor.getValue().isVerified()).isFalse();
    }

    @Test
    @DisplayName("실패 횟수가 한도에 닿으면 저장값을 지우고 횟수 초과 예외를 던진다")
    void 실패_한도에_닿으면_삭제한다() {
        PasswordReset almostLocked = new PasswordReset(
                EMAIL, CODE, false, PasswordResetServiceImpl.MAX_FAILED_ATTEMPTS - 1);
        when(passwordResetRepository.findById(EMAIL)).thenReturn(Optional.of(almostLocked));

        assertThatThrownBy(() -> passwordResetService.verifyCode(EMAIL, "BWJFAS"))
                .isInstanceOf(EmailCodeAttemptsExceededException.class);

        verify(passwordResetRepository).deleteById(EMAIL);
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장된 코드가 없으면(발송 안 함 또는 5분 만료) 예외를 던진다")
    void 저장된_코드가_없으면_예외를_던진다() {
        when(passwordResetRepository.findById(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.verifyCode(EMAIL, CODE))
                .isInstanceOf(EmailCodeMismatchException.class);

        verify(passwordResetRepository, never()).save(any());
    }

    // ---------- 3단계: 재설정 ----------

    @Test
    @DisplayName("인증이 끝난 이메일이면 비밀번호를 바꾸고 refresh 토큰과 인증 정보를 지운다")
    void 인증_완료면_비밀번호를_바꾼다() {
        User user = localUser();
        when(passwordResetRepository.findById(EMAIL))
                .thenReturn(Optional.of(PasswordReset.issue(EMAIL, CODE).markVerified()));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("newHash");

        passwordResetService.resetPassword(new PasswordResetRequest(EMAIL, NEW_PASSWORD, NEW_PASSWORD));

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        verify(refreshTokenRepository).deleteById("GUARDIAN:1");
        verify(passwordResetRepository).deleteById(EMAIL);
    }

    @Test
    @DisplayName("코드 확인을 하지 않았으면 비밀번호를 바꾸지 않는다")
    void 인증_전이면_예외를_던진다() {
        when(passwordResetRepository.findById(EMAIL))
                .thenReturn(Optional.of(PasswordReset.issue(EMAIL, CODE)));

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new PasswordResetRequest(EMAIL, NEW_PASSWORD, NEW_PASSWORD)))
                .isInstanceOf(EmailNotVerifiedException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(passwordResetRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("새 비밀번호와 확인 값이 다르면 인증 정보를 소모하지 않고 예외를 던진다")
    void 비밀번호_확인이_다르면_예외를_던진다() {
        doThrow(new PasswordMismatchException())
                .when(userValidationPolicy).validatePasswordMatch(NEW_PASSWORD, "different1234");

        assertThatThrownBy(() -> passwordResetService.resetPassword(
                new PasswordResetRequest(EMAIL, NEW_PASSWORD, "different1234")))
                .isInstanceOf(PasswordMismatchException.class);

        verify(passwordResetRepository, never()).deleteById(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
