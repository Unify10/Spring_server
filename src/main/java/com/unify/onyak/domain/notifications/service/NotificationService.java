package com.unify.onyak.domain.notifications.service;

import com.unify.onyak.domain.notifications.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotifications(Long userId, String role);
}
