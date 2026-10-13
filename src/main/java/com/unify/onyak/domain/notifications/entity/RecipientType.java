package com.unify.onyak.domain.notifications.entity;

import com.unify.onyak.domain.notifications.error.exception.InvalidRecipientRoleException;

public enum RecipientType {
    GUARDIAN, WARD;

    public static RecipientType from(String role) {
        if (role == null) {
            throw new InvalidRecipientRoleException();
        }

        try {
            return RecipientType.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new InvalidRecipientRoleException();
        }
    }
}
