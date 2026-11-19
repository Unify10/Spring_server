package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class EmailNotVerifiedException extends CustomException {
    public EmailNotVerifiedException() {
        super(AuthErrorCode.AUTH_EMAIL_NOT_VERIFIED);
    }
}
