package com.unify.onyak.domain.notifications.repository;

import com.unify.onyak.domain.notifications.entity.Notification;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Slice<Notification> findByRecipientTypeAndRecipientId(
            RecipientType recipientType, Long recipientId, Pageable pageable);
}
