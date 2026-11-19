package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.service.UserSignUpService;
import com.unify.onyak.domain.users.auth.dto.request.SignUpRequest;
import com.unify.onyak.domain.users.auth.dto.response.SignUpResponse;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.auth.service.AuthService;
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
    private final AuthService authService;

    @Transactional
    @Override
    public SignUpResponse signUp(SignUpRequest request) {

        // 비밀번호와 비밀번호 확인 체크
        userValidationPolicy.validatePasswordMatch(request.password(), request.passwordConfirm());

        // 이메일 중복 검사
        userValidationPolicy.validateDuplicateEmail(request.email());

        // 이메일 인증 완료 여부 검사
        userValidationPolicy.validateEmailVerified(request.email());

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
                request.isPregnant(),
                request.medicationPushEnabled() == null ||  request.medicationPushEnabled(),
                request.sosPushEnabled() == null ||  request.sosPushEnabled(),
                request.marketingPushEnabled() == null || request.marketingPushEnabled()
        );

        userRepository.save(user);

        // 가입 직후 자동 로그인: GUARDIAN 토큰 발급 + Redis refresh 저장
        TokenResponse tokens = authService.issueTokens(user.getId(), Role.GUARDIAN);

        return new SignUpResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                tokens.accessToken(),
                tokens.refreshToken()
        );
    }
}
