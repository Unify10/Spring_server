package com.unify.onyak.domain.wards.error.exception;

import com.unify.onyak.domain.wards.error.WardAuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class WardAccessDeniedException extends CustomException {
    public WardAccessDeniedException() {
        super(WardAuthErrorCode.WARD_ACCESS_DENIED);
    }
}
