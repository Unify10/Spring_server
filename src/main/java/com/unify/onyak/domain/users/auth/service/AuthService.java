package com.unify.onyak.domain.users.auth.service;

// interface
public interface AuthService {
    void logout(Long userId, String role);
}