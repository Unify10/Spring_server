package com.unify.onyak.domain.users.profile.error.exception;

import com.unify.onyak.domain.users.profile.error.ProfileErrorCode;
import com.unify.onyak.global.error.CustomException;

public class CurrentPasswordMismatch extends CustomException {
    public CurrentPasswordMismatch() {
        super(ProfileErrorCode.CURRENT_PASSWORD_MISMATCH);
    }
}
