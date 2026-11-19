package com.unify.onyak.domain.users.auth.service;

public interface EmailVerificationService {

    void sendCode(String email);

    void verifyCode(String email, String code);
}
