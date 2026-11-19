package com.unify.onyak.domain.notifications.error.exception;

import com.unify.onyak.global.error.CustomException;

public class SosForbiddenException extends CustomException {
    public SosForbiddenException() {
        super(NotificationErrorCode.SOS_FORBIDDEN);
    }
}