package com.unify.onyak.domain.wards.profile.error.exception;

import com.unify.onyak.domain.wards.profile.error.WardProfileErrorCode;
import com.unify.onyak.global.error.CustomException;

public class WardUnauthorized extends CustomException {
    public WardUnauthorized() {
        super(WardProfileErrorCode.WARD_UNAUTHORIZED);
    }
}
