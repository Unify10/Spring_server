package com.unify.onyak.domain.users.auth.service.impl;

import com.unify.onyak.domain.users.auth.dto.request.LoginRequest;
import com.unify.onyak.domain.users.auth.dto.request.RefreshRequest;
import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.InvalidTokenException;
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
        TokenResponse tokens = issueTokens(user.getId(), role);

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                role,
                tokens.accessToken(),
                tokens.refreshToken()
        );
    }

    @Override
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();

        jwtProvider.validateToken(refreshToken, "refresh");
        Long userId = jwtProvider.getUserId(refreshToken);
        Role role = jwtProvider.getRole(refreshToken);

        RefreshToken saved = refreshTokenRepository.findById(RefreshToken.keyOf(role, userId))
                .orElseThrow(InvalidTokenException::new);

        if (!saved.getRefreshToken().equals(refreshToken)) {
            throw new InvalidTokenException();
        }

        return issueTokens(userId, role);
    }

    @Override
    public void logout(Long userId, Role role) {
        refreshTokenRepository.deleteById(RefreshToken.keyOf(role, userId));
    }

    @Override
    public TokenResponse issueTokens(Long userId, Role role) {
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId, role);

        refreshTokenRepository.save(new RefreshToken(
                userId,
                role,
                refreshToken,
                jwtProvider.getRefreshTokenExpTime()
        ));

        return new TokenResponse(accessToken, refreshToken);
    }
}
