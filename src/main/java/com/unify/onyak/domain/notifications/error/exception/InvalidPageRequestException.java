package com.unify.onyak.domain.notifications.error.exception;

import com.unify.onyak.global.error.CustomException;

public class InvalidPageRequestException extends CustomException {
    public InvalidPageRequestException() {
        super(NotificationErrorCode.INVALID_PAGE_REQUEST);
    }
}
