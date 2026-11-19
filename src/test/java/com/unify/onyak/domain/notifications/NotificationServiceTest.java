package com.unify.onyak.domain.notifications;

import com.unify.onyak.domain.notifications.dto.response.NotificationResponse;
import com.unify.onyak.domain.notifications.entity.Notification;
import com.unify.onyak.domain.notifications.entity.NotificationType;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.error.exception.InvalidPageRequestException;
import com.unify.onyak.domain.notifications.repository.NotificationRepository;
import com.unify.onyak.domain.notifications.service.NotificationService;
import com.unify.onyak.domain.notifications.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        when(notificationRepository.findByRecipientTypeAndRecipientId(
                eq(RecipientType.GUARDIAN), eq(3L), any(Pageable.class))).thenReturn(slice(sos));

        List<NotificationResponse> result = notificationService.getNotifications(3L, RecipientType.GUARDIAN, 0, 20);

        assertEquals(1, result.size());
        assertEquals(NotificationType.SOS, result.get(0).type());
        assertEquals(37.5, result.get(0).locationLat());
        assertFalse(result.get(0).isRead());
    }

    @Test
    void 알림이_없으면_빈_목록을_반환한다() {
        when(notificationRepository.findByRecipientTypeAndRecipientId(
                eq(RecipientType.WARD), eq(7L), any(Pageable.class))).thenReturn(slice());

        assertTrue(notificationService.getNotifications(7L, RecipientType.WARD, 0, 20).isEmpty());
    }

    @Test
    void 요청한_페이지와_크기로_createdAt_id_내림차순_정렬해_조회한다() {
        when(notificationRepository.findByRecipientTypeAndRecipientId(
                eq(RecipientType.GUARDIAN), eq(3L), any(Pageable.class))).thenReturn(slice());

        notificationService.getNotifications(3L, RecipientType.GUARDIAN, 1, 5);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByRecipientTypeAndRecipientId(
                eq(RecipientType.GUARDIAN), eq(3L), captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("id").getDirection());
        assertEquals(List.of("createdAt", "id"),
                pageable.getSort().stream().map(Sort.Order::getProperty).toList());
    }

    @Test
    void size가_최대값_100이면_허용한다() {
        when(notificationRepository.findByRecipientTypeAndRecipientId(
                any(), any(), any(Pageable.class))).thenReturn(slice());

        assertDoesNotThrow(() -> notificationService.getNotifications(3L, RecipientType.GUARDIAN, 0, 100));
    }

    @Test
    void 잘못된_page_size_이면_조회하지_않고_예외를_던진다() {
        assertThrows(InvalidPageRequestException.class,
                () -> notificationService.getNotifications(3L, RecipientType.GUARDIAN, -1, 20));
        assertThrows(InvalidPageRequestException.class,
                () -> notificationService.getNotifications(3L, RecipientType.GUARDIAN, 0, 0));
        assertThrows(InvalidPageRequestException.class,
                () -> notificationService.getNotifications(3L, RecipientType.GUARDIAN, 0, 101));

        verifyNoInteractions(notificationRepository);
    }

    private Slice<Notification> slice(Notification... notifications) {
        return new SliceImpl<>(List.of(notifications));
    }
}
