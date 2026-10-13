package com.unify.onyak.domain.notifications.service.impl;

import com.unify.onyak.domain.notifications.dto.response.NotificationResponse;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.repository.NotificationRepository;
import com.unify.onyak.domain.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId, RecipientType recipientType) {
        return notificationRepository
                .findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(recipientType, userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}