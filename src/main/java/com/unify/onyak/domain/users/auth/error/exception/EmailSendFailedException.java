package com.unify.onyak.domain.users.auth.error.exception;

import com.unify.onyak.domain.users.auth.error.AuthErrorCode;
import com.unify.onyak.global.error.CustomException;

public class EmailSendFailedException extends CustomException {
    public EmailSendFailedException() {
        super(AuthErrorCode.AUTH_EMAIL_SEND_FAILED);
    }
}
