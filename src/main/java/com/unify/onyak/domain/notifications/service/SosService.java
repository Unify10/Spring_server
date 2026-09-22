package com.unify.onyak.domain.notifications.service;

import com.unify.onyak.domain.notifications.dto.request.SosRequest;
import com.unify.onyak.domain.notifications.entity.Notification;
import com.unify.onyak.domain.notifications.repository.NotificationRepository;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SosService {

    private final NotificationRepository notificationRepository;
    private final WardRepository wardRepository; // 윤범이가 작업해야하지만 내가 작업함
    // private final PushNotificationService pushNotificationService; // TODO: FCM 푸시 연동 시 사용

    @Transactional
    public void sendSos(Long wardId, SosRequest request) {
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 피보호자를 찾을 수 없습니다."));

        Long guardianId = ward.getGuardianId().getId();
        String message = ward.getName() + "님의 긴급 SOS 요청이 발생했습니다.";

        Notification sosNotification = Notification.createSos(
                guardianId,
                message,
                request.getLocationLat(),
                request.getLocationLng()
        );
        notificationRepository.save(sosNotification);

        //  실제 푸시 알림 발송
        // pushNotificationService.sendToGuardian(guardianId, message);
    }
}