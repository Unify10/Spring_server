package com.unify.onyak.domain.users.auth.service;

import com.unify.onyak.domain.users.auth.dto.request.LoginRequest;
import com.unify.onyak.domain.users.auth.dto.request.RefreshRequest;
import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.dto.response.TokenResponse;
import com.unify.onyak.domain.users.auth.entity.Role;

// interface
public interface AuthService {
    LoginResponse login(LoginRequest request);

    TokenResponse refresh(RefreshRequest request);

    // 가입·로그인·재발급 공통: access/refresh 발급 + Redis(refresh:{role}:{id}) 저장
    TokenResponse issueTokens(Long userId, Role role);

    void logout(Long userId, Role role);
}
