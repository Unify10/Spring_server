package com.unify.onyak.domain.users.auth.policy;

import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.LoginFailedException;
import com.unify.onyak.domain.users.auth.repository.EmailVerificationRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserValidationPolicyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @InjectMocks
    private UserValidationPolicy userValidationPolicy;

    @Test
    @DisplayName("구글 전용 계정(password_hash=NULL)으로 비밀번호 로그인 시 AUTH_LOGIN_FAILED(LoginFailedException) 발생")
    void 구글전용계정_비밀번호로그인_실패() {
        // given
        String email = "google-only@example.com";
        User googleOnlyUser = User.createGoogle(email, "홍길동", "google-sub-123");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(googleOnlyUser));

        // when & then
        assertThatThrownBy(() -> userValidationPolicy.validateLogin(email, "rawPassword1234"))
                .isInstanceOf(LoginFailedException.class);

        // passwordEncoder.matches가 null 인자로 호출되지 않았는지 확인
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
