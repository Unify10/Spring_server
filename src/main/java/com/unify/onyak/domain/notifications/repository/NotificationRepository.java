package com.unify.onyak.domain.notifications.repository;

import com.unify.onyak.domain.notifications.entity.Notification;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
            RecipientType recipientType, Long recipientId);
}
