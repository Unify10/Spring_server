package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class LoginFailedException extends CustomException {
    public LoginFailedException() {
        super(AuthErrorCode.AUTH_LOGIN_FAILED);
    }
}
