package com.unify.onyak.domain.notifications.service.impl;

import com.unify.onyak.domain.notifications.dto.response.NotificationResponse;
import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.error.exception.InvalidPageRequestException;
import com.unify.onyak.domain.notifications.repository.NotificationRepository;
import com.unify.onyak.domain.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId, RecipientType recipientType, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidPageRequestException();
        }

        // createdAt 이 같은 알림끼리도 순서가 흔들리지 않도록 id 를 보조 정렬로 사용
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        return notificationRepository
                .findByRecipientTypeAndRecipientId(recipientType, userId, pageable)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
