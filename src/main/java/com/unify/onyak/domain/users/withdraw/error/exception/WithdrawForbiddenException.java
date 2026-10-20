package com.unify.onyak.domain.users.withdraw.error.exception;

import com.unify.onyak.domain.users.withdraw.error.WithdrawErrorCode;
import com.unify.onyak.global.error.CustomException;

public class WithdrawForbiddenException extends CustomException {
    public WithdrawForbiddenException() {
        super(WithdrawErrorCode.WITHDRAW_FORBIDDEN);
    }
}