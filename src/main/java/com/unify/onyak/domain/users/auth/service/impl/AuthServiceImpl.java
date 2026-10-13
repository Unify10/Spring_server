package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.request.LoginRequest;
import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.policy.UserValidationPolicy;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.service.AuthService;
import com.unify.onyak.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtProvider jwtProvider;
    private final UserValidationPolicy userValidationPolicy;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userValidationPolicy.validateLogin(request.email(), request.password());
        Role role = Role.GUARDIAN;
        String accessToken = jwtProvider.generateAccessToken(user.getId(), role);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), role);

        RefreshToken token = new RefreshToken(
                user.getId(),
                role,
                refreshToken,
                jwtProvider.getRefreshTokenExpTime()
        );

        refreshTokenRepository.save(token);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(Long userId) {
        refreshTokenRepository.deleteById(userId);
    }
}