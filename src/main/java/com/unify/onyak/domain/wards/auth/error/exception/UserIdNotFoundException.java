package com.unify.onyak.domain.wards.auth.error.exception;

import com.unify.onyak.domain.wards.auth.error.WardAuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class UserIdNotFoundException extends CustomException{
    public UserIdNotFoundException() {
        super(WardAuthErrorCode.AUTH_ID_NOT_FOUND);
    }
}
