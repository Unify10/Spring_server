package com.unify.onyak.domain.notifications.dto.response;

import com.unify.onyak.domain.notifications.entity.Notification;
import com.unify.onyak.domain.notifications.entity.NotificationType;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        Double locationLat,
        Double locationLng,
        Boolean isRead,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getLocationLat(),
                n.getLocationLng(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }
}