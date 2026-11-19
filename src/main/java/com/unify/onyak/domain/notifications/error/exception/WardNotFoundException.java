package com.unify.onyak.domain.notifications.error.exception;

import com.unify.onyak.global.error.CustomException;

public class WardNotFoundException extends CustomException {
    public WardNotFoundException() {
        super(NotificationErrorCode.WARD_NOT_FOUND);
    }
}
