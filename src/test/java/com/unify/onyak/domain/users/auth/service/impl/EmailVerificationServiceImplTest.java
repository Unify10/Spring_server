package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.entity.EmailVerification;
import com.unify.onyak.domain.users.auth.error.exception.EmailCodeMismatchException;
import com.unify.onyak.domain.users.auth.repository.EmailVerificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    private static final String EMAIL = "test@example.com";
    private static final String CODE = "K617WW";

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    @Test
    @DisplayName("저장된 코드와 같으면 인증 완료(verified=true)로 저장한다")
    void 코드가_일치하면_인증_완료로_저장한다() {
        when(emailVerificationRepository.findById(EMAIL))
                .thenReturn(Optional.of(new EmailVerification(EMAIL, CODE, false)));

        emailVerificationService.verifyCode(EMAIL, CODE);

        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(captor.getValue().getCode()).isEqualTo(CODE);
        assertThat(captor.getValue().isVerified()).isTrue();
    }

    @Test
    @DisplayName("저장된 코드와 다르면 예외를 던지고 저장하지 않는다 (재발송 전의 옛 코드 포함)")
    void 코드가_다르면_예외를_던지고_저장하지_않는다() {
        when(emailVerificationRepository.findById(EMAIL))
                .thenReturn(Optional.of(new EmailVerification(EMAIL, CODE, false)));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "BWJFAS"))
                .isInstanceOf(EmailCodeMismatchException.class);

        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("코드가 대소문자만 다른 경우도 일치하지 않는 것으로 본다")
    void 대소문자가_다르면_예외를_던진다() {
        when(emailVerificationRepository.findById(EMAIL))
                .thenReturn(Optional.of(new EmailVerification(EMAIL, CODE, false)));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, CODE.toLowerCase()))
                .isInstanceOf(EmailCodeMismatchException.class);

        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("저장된 인증 정보가 없으면(발송 안 함 또는 5분 만료) 예외를 던진다")
    void 저장된_코드가_없으면_예외를_던진다() {
        when(emailVerificationRepository.findById(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, CODE))
                .isInstanceOf(EmailCodeMismatchException.class);

        verify(emailVerificationRepository, never()).save(any());
    }
}
