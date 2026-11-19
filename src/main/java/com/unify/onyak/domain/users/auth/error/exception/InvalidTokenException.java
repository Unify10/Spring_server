package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class InvalidTokenException extends CustomException {
    public InvalidTokenException() {
        super(AuthErrorCode.AUTH_INVALID_TOKEN);
    }
}
