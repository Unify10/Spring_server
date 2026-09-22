package com.unify.onyak.domain.users.profile.service.impl;

import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;
import com.unify.onyak.domain.users.profile.service.SettingEditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingEditImpl implements SettingEditService {

    @Override
    public SettingEditResponse editSetting(SettingEditRequest request) {
        Boolean medicationPush = null;

        Boolean sosPush = null;

        Boolean marketingPush = null;

        if(request.medicationPushEnabled() != null)
            medicationPush = !request.medicationPushEnabled();

        if(request.sosPushEnabled() != null)
            sosPush = !request.sosPushEnabled();

        if(request.marketingPushEnabled() != null)
            marketingPush = !request.marketingPushEnabled();

        System.out.println(request.medicationPushEnabled() + " " + request.sosPushEnabled() + " " + request.marketingPushEnabled());
        return SettingEditResponse.builder()
                .medicationPushEnabled(medicationPush)
                .sosPushEnabled(sosPush)
                .marketingPushEnabled(marketingPush)
                .build();
    }
}
