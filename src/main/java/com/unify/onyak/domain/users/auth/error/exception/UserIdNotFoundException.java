package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class UserIdNotFoundException extends CustomException {
    public UserIdNotFoundException() {super(AuthErrorCode.AUTH_ID_NOTFOUND);}
}
