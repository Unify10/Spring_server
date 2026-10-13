package com.unify.onyak.domain.notifications.error.exception;

import com.unify.onyak.global.error.CustomException;

public class InvalidRecipientRoleException extends CustomException {
    public InvalidRecipientRoleException() {
        super(NotificationErrorCode.INVALID_RECIPIENT_ROLE);
    }
}
