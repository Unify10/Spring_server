package com.unify.onyak.domain.notifications;

import com.unify.onyak.domain.notifications.dto.request.SosRequest;
import com.unify.onyak.domain.notifications.entity.Notification;
import com.unify.onyak.domain.notifications.entity.NotificationType;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.error.exception.WardNotFoundException;
import com.unify.onyak.domain.notifications.repository.NotificationRepository;
import com.unify.onyak.domain.notifications.service.SosService;
import com.unify.onyak.domain.notifications.service.impl.SosServiceImpl;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SosServiceTest {

    NotificationRepository notificationRepository;
    WardRepository wardRepository;
    SosService sosService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        wardRepository = mock(WardRepository.class);
        sosService = new SosServiceImpl(wardRepository, notificationRepository);
    }

    @Test
    void 존재하지_않는_피보호자면_예외를_던진다() {
        when(wardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(WardNotFoundException.class,
                () -> sosService.sendSos(999L, new SosRequest(null , null)));

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void 피보호자의_보호자에게_SOS_알림을_저장한다() {
        User guardian = mock(User.class);
        when(guardian.getId()).thenReturn(3L);

        Ward ward = mock(Ward.class);
        when(ward.getGuardianId()).thenReturn(guardian);
        when(ward.getName()).thenReturn("철수");

        when(wardRepository.findById(7L)).thenReturn(Optional.of(ward));

        SosRequest request = new SosRequest(null, null);
        sosService.sendSos(7L, request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(RecipientType.GUARDIAN, saved.getRecipientType());
        assertEquals(3L, saved.getRecipientId());
        assertEquals(NotificationType.SOS, saved.getType());
        assertTrue(saved.getMessage().contains("철수"));
    }
}
