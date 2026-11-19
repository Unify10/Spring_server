package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class DuplicateEmailException extends CustomException {
    public DuplicateEmailException() {
        super(AuthErrorCode.AUTH_EMAIL_DUPLICATE);
    }
}
