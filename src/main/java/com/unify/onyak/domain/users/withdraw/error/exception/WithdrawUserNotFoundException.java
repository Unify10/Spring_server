package com.unify.onyak.domain.users.withdraw.error.exception;

import com.unify.onyak.domain.users.withdraw.error.WithdrawErrorCode;
import com.unify.onyak.global.error.CustomException;

public class WithdrawUserNotFoundException extends CustomException {
    public WithdrawUserNotFoundException() {
        super(WithdrawErrorCode.WITHDRAW_USER_NOT_FOUND);
    }
}