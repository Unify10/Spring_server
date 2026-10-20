package com.unify.onyak.domain.notifications.service;

import com.unify.onyak.domain.notifications.dto.response.NotificationResponse;
import com.unify.onyak.domain.notifications.entity.RecipientType;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotifications(Long userId, RecipientType recipientType, int page, int size);
}
