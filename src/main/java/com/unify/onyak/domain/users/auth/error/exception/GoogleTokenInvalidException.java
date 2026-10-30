package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class GoogleTokenInvalidException extends CustomException {
    public GoogleTokenInvalidException() {
        super(AuthErrorCode.AUTH_GOOGLE_TOKEN_INVALID);
    }
}
