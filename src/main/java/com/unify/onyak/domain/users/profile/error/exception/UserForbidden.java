package com.unify.onyak.domain.users.profile.error.exception;

import com.unify.onyak.domain.users.profile.error.ProfileErrorCode;
import com.unify.onyak.global.error.CustomException;

public class UserForbidden extends CustomException {
    public UserForbidden() { super(ProfileErrorCode.USER_FORBIDDEN); }
}
