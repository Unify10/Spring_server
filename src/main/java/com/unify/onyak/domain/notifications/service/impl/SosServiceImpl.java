package com.unify.onyak.domain.notifications.service.impl;

import com.unify.onyak.domain.notifications.entity.Notification;
import com.unify.onyak.domain.notifications.error.exception.WardNotFoundException;
import com.unify.onyak.domain.notifications.service.SosService;
import com.unify.onyak.domain.wards.auth.entity.Ward;

import com.unify.onyak.domain.notifications.repository.NotificationRepository;

import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unify.onyak.domain.notifications.dto.request.SosRequest;

import com.unify.onyak.domain.notifications.error.exception.SosForbiddenException;
import com.unify.onyak.domain.users.auth.entity.Role;

@Service
@RequiredArgsConstructor
public class SosServiceImpl implements SosService {

    private final WardRepository wardRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void sendSos(Long wardId, Role role, SosRequest request) {
        if (role != Role.WARD) {
            throw new SosForbiddenException();
        }

        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(WardNotFoundException::new);

        Long guardianId = ward.getGuardianId().getId();
        String message = ward.getName() + "님의 긴급 SOS 요청이 발생했습니다.";

        Notification sosNotification = Notification.createSos(
                guardianId,
                message,
                request.locationLat(),
                request.locationLng()
        );

        notificationRepository.save(sosNotification);

        // 실제 푸시 알림 발송은 추후 연동
        // pushNotificationService.sendToGuardian(guardianId, message);
    }
}
