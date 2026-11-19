package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class PasswordMismatchException extends CustomException {
    public PasswordMismatchException() {
        super(AuthErrorCode.AUTH_PASSWORD_MISMATCH);
    }
}
