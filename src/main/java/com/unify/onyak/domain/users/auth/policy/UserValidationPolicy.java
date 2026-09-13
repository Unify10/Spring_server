package com.unify.onyak.domain.users.auth.policy;

import com.unify.onyak.domain.users.auth.error.exception.DuplicateEmailException;
import com.unify.onyak.domain.users.auth.error.exception.PasswordMismatchException;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserValidationPolicy {
    private final UserRepository userRepository;

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
}
