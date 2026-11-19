package com.unify.onyak.domain.users.profile.error.exception;

import com.unify.onyak.domain.users.profile.error.ProfileErrorCode;
import com.unify.onyak.global.error.CustomException;

public class UserNotFound extends CustomException {
    public UserNotFound() { super(ProfileErrorCode.USER_NOT_FOUND); }
}
