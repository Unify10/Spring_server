package com.unify.onyak.domain.users.auth.policy;

import com.unify.onyak.domain.users.auth.entity.EmailVerification;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.DuplicateEmailException;
import com.unify.onyak.domain.users.auth.error.exception.EmailNotVerifiedException;
import com.unify.onyak.domain.users.auth.error.exception.LoginFailedException;
import com.unify.onyak.domain.users.auth.error.exception.PasswordMismatchException;
import com.unify.onyak.domain.users.auth.repository.EmailVerificationRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserValidationPolicy {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final EmailVerificationRepository emailVerificationRepository;

    public void validateDuplicateEmail(String email) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();// 중복이 있다면 커스텀 에러 처리
        }
    }

    public void validatePasswordMatch(String password, String passwordConfirm) {

        // 비밀번호와 비밀번호 확인 동일 체크
        if (!password.equals(passwordConfirm)) {
            throw new PasswordMismatchException();
        }
    }

    public void validateEmailVerified(String email) {

        // 이메일 인증 완료 여부 체크
        boolean verified = emailVerificationRepository.findById(email)
                .map(EmailVerification::isVerified)
                .orElse(false);

        if (!verified) {
            throw new EmailNotVerifiedException();
        }
    }

    public User validateLogin(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(LoginFailedException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new LoginFailedException();
        }

        return user;
    }

}
