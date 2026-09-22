package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class TokenExpiredException extends CustomException {
    public TokenExpiredException() {
        super(AuthErrorCode.AUTH_TOKEN_EXPIRED);
    }
}
