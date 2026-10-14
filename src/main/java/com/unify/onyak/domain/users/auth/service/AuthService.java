package com.unify.onyak.domain.users.auth.service;

import com.unify.onyak.domain.users.auth.dto.request.LoginRequest;
import com.unify.onyak.domain.users.auth.dto.request.RefreshRequest;
import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;
import com.unify.onyak.domain.users.auth.entity.Role;

// interface
public interface AuthService {
    LoginResponse login(LoginRequest request);

    LoginResponse refresh(RefreshRequest request);

    void logout(Long userId, Role role);
}
