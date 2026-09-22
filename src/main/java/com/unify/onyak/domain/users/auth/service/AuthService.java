package com.unify.onyak.domain.users.auth.service;

import com.unify.onyak.domain.users.auth.dto.request.LoginRequest;
import com.unify.onyak.domain.users.auth.dto.response.LoginResponse;

// interface
public interface AuthService {
    LoginResponse login(LoginRequest request);

    void logout(Long userId);
}