package com.unify.onyak.domain.wards.profile.error.exception;

import com.unify.onyak.domain.wards.profile.error.WardProfileErrorCode;
import com.unify.onyak.global.error.CustomException;

public class WardNotFound extends CustomException {
    public WardNotFound() {
        super(WardProfileErrorCode.WARD_NOT_FOUND);
    }
}
