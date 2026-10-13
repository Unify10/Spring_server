package com.unify.onyak.domain.wards.auth.error.exception;

import com.unify.onyak.domain.wards.auth.error.WardAuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class GuardianIdMismatchException extends CustomException {
    public GuardianIdMismatchException(){
        super(WardAuthErrorCode.GUARDIAN_ID_MISMATCH);
    }
}
