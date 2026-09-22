package com.unify.onyak.domain.users.profile.service;

import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;

public interface SettingEditService {
    SettingEditResponse editSetting(SettingEditRequest request);
}
