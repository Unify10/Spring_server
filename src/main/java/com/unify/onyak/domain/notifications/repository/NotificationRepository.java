package com.unify.onyak.domain.notifications.repository;

import com.unify.onyak.domain.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
}
