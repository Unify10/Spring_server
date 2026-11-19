package com.unify.onyak.domain.wards.error.exception;

import com.unify.onyak.domain.wards.error.WardAuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class GuardianRoleRequiredException extends CustomException {
    public GuardianRoleRequiredException() {
        super(WardAuthErrorCode.GUARDIAN_ROLE_REQUIRED);
    }

}
