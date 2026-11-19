package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class EmailCodeMismatchException extends CustomException {
    public EmailCodeMismatchException() {
        super(AuthErrorCode.AUTH_EMAIL_CODE_MISMATCH);
    }
}
