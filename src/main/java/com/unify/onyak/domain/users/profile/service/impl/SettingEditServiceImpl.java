package com.unify.onyak.domain.users.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;
import com.unify.onyak.domain.users.profile.error.exception.UserForbidden;
import com.unify.onyak.domain.users.profile.error.exception.UserNotFound;
import com.unify.onyak.domain.users.profile.service.SettingEditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingEditServiceImpl implements SettingEditService {

    private final UserRepository userRepository;

    @Transactional
    @Override
    public SettingEditResponse editSetting(Long userId, Role role, SettingEditRequest request) {

        if(role != Role.GUARDIAN){
            throw new UserForbidden();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFound::new);

        user.updateSettings(
                request.medicationPushEnabled(),
                request.sosPushEnabled(),
                request.marketingPushEnabled()
        );

        return SettingEditResponse.builder()
                .medicationPushEnabled(user.getMedicationPushEnabled())
                .sosPushEnabled(user.getSosPushEnabled())
                .marketingPushEnabled(user.getMarketingPushEnabled())
                .build();
    }
}
