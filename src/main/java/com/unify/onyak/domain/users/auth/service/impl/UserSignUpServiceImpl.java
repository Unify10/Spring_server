package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.domain.users.auth.dto.request.SignUpRequest;
import com.unify.onyak.domain.users.auth.dto.response.SignUpResponse;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserSignUpServiceImpl implements UserSignUpService {

    private final UserRepository userRepository;
    private final UserValidationPolicy userValidationPolicy;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public SignUpResponse signUp(SignUpRequest request) {

        // 비밀번호와 비밀번호 확인 체크
        userValidationPolicy.validatePasswordMatch(request.password(), request.passwordConfirm());

        // 이메일 중복 검사
        userValidationPolicy.validateDuplicateEmail(request.email());

        String password = passwordEncoder.encode(request.password());

        User user = User.createLocal(
                request.email(),
                password,
                request.name(),
                request.phone(),
                request.birthDate(),
                request.weight(),
                request.height(),
                request.gender(),
                request.medicationPushEnabled(),
                request.sosPushEnabled()
        );

        userRepository.save(user);

        return new SignUpResponse(user.getId(), user.getEmail(), user.getName());
    }
}
