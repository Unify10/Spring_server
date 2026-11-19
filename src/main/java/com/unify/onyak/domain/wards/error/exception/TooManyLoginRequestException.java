package com.unify.onyak.domain.wards.error.exception;

import com.unify.onyak.domain.wards.error.WardAuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class TooManyLoginRequestException extends CustomException {
    public TooManyLoginRequestException() {
        super(WardAuthErrorCode.TOO_MANY_LOGIN_REQUESTS);
    }
}
