package com.unify.onyak.domain.notifications;

import com.unify.onyak.domain.notifications.dto.response.NotificationResponse;
import com.unify.onyak.domain.notifications.entity.Notification;
import com.unify.onyak.domain.notifications.entity.NotificationType;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.repository.NotificationRepository;
import com.unify.onyak.domain.notifications.service.NotificationService;
import com.unify.onyak.domain.notifications.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    NotificationRepository notificationRepository;
    NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationService = new NotificationServiceImpl(notificationRepository);
    }

    @Test
    void 역할과_ID로_조회해_응답_DTO로_변환한다() {
        Notification sos = Notification.createSos(3L, "철수님의 긴급 SOS 요청이 발생했습니다.", 37.5, 127.0);
        when(notificationRepository.findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
                RecipientType.GUARDIAN, 3L)).thenReturn(List.of(sos));

        List<NotificationResponse> result = notificationService.getNotifications(3L, "GUARDIAN");

        assertEquals(1, result.size());
        assertEquals(NotificationType.SOS, result.get(0).type());
        assertEquals(37.5, result.get(0).locationLat());
        assertFalse(result.get(0).isRead());
    }

    @Test
    void 알림이_없으면_빈_목록을_반환한다() {
        when(notificationRepository.findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
                RecipientType.WARD, 7L)).thenReturn(List.of());

        assertTrue(notificationService.getNotifications(7L, "WARD").isEmpty());
    }
}