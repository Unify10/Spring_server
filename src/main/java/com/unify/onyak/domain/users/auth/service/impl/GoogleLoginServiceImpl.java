package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.GoogleUserInfo;
import com.unify.onyak.domain.users.auth.dto.request.GoogleLoginRequest;
import com.unify.onyak.domain.users.auth.dto.response.GoogleLoginResponse;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.DuplicateEmailException;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.domain.users.auth.service.GoogleIdTokenVerifier;
import com.unify.onyak.domain.users.auth.service.GoogleLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleLoginServiceImpl implements GoogleLoginService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    private final UserRepository userRepository;

    private final AuthService authService;

    @Transactional
    @Override
    public GoogleLoginResponse login(GoogleLoginRequest request) {
        GoogleUserInfo userInfo = googleIdTokenVerifier.verify(request.idToken());

        // 1. google_id로 기존 사용자 확인
        Optional<User> userByGoogleId = userRepository.findByGoogleId(userInfo.googleId());
        if (userByGoogleId.isPresent()) {
            User user = userByGoogleId.get();
            TokenResponse tokens = authService.issueTokens(user.getId(), Role.GUARDIAN);
            return toResponse(user, tokens, false);
        }

        // 2. 같은 email의 기존 계정 확인
        Optional<User> userByEmail = userRepository.findByEmail(userInfo.email());
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new DuplicateEmailException();
            }
            user.linkGoogleId(userInfo.googleId());
            try {
                userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException e) {
                throw new DuplicateEmailException();
            }

            TokenResponse tokens = authService.issueTokens(user.getId(), Role.GUARDIAN);
            return toResponse(user, tokens, false);
        }

        // 3. 신규 가입
        User newUser = User.createGoogle(userInfo.email(), userInfo.name(), userInfo.googleId());
        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }

        TokenResponse tokens = authService.issueTokens(savedUser.getId(), Role.GUARDIAN);
        return toResponse(savedUser, tokens, true);
    }

    private GoogleLoginResponse toResponse(User user, TokenResponse tokens, boolean isNewUser) {
        return new GoogleLoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                Role.GUARDIAN,
                tokens.accessToken(),
                tokens.refreshToken(),
                isNewUser
        );
    }
}
