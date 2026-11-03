package com.unify.onyak.domain.users.profile.error.exception;

import com.unify.onyak.domain.users.profile.error.ProfileErrorCode;
import com.unify.onyak.global.error.CustomException;

public class InvalidPregnancy extends CustomException {
    public InvalidPregnancy() { super(ProfileErrorCode.INVALID_PREGNANCY); }
}