package com.unify.onyak.domain.users.auth.service;

import com.unify.onyak.domain.users.auth.dto.request.PasswordResetRequest;

public interface PasswordResetService {

    void sendCode(String email);

    void verifyCode(String email, String code);

    void resetPassword(PasswordResetRequest request);
}
