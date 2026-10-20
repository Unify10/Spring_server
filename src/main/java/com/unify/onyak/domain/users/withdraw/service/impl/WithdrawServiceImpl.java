package com.unify.onyak.domain.users.withdraw.service.impl;

import com.unify.onyak.domain.notifications.entity.RecipientType;
import com.unify.onyak.domain.notifications.repository.NotificationRepository;
import com.unify.onyak.domain.users.auth.entity.RefreshToken;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.RefreshTokenRepository;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.withdraw.error.exception.WithdrawForbiddenException;
import com.unify.onyak.domain.users.auth.error.exception.UserIdNotFoundException;
import com.unify.onyak.domain.users.withdraw.service.WithdrawService;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl implements WithdrawService {

    private final UserRepository userRepository;
    private final WardRepository wardRepository;
    private final NotificationRepository notificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public void withdraw(Long userId, Role role) {
        if (role != Role.GUARDIAN) {
            throw new WithdrawForbiddenException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserIdNotFoundException::new);

        List<Ward> wards = wardRepository.findByGuardianId_Id(userId);

        for (Ward ward : wards) {
            notificationRepository.deleteByRecipientTypeAndRecipientId(RecipientType.WARD, ward.getId());
        }
        notificationRepository.deleteByRecipientTypeAndRecipientId(RecipientType.GUARDIAN, userId);

        wardRepository.deleteAll(wards);
        userRepository.delete(user);

        for (Ward ward : wards) {
            refreshTokenRepository.deleteById(RefreshToken.keyOf(Role.WARD, ward.getId()));
        }
        refreshTokenRepository.deleteById(RefreshToken.keyOf(Role.GUARDIAN, userId));
    }
}