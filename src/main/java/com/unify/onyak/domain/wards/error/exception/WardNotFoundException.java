package com.unify.onyak.domain.wards.error.exception;

import com.unify.onyak.domain.wards.error.WardAuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class WardNotFoundException extends CustomException {
    public WardNotFoundException() {
        super(WardAuthErrorCode.WARD_NOT_FOUNT);
    }
}
