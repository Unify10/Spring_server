package com.unify.onyak.domain.users.profile.service;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.profile.dto.request.SettingEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.SettingEditResponse;

public interface SettingEditService {
    SettingEditResponse editSetting(Long userId, Role role, SettingEditRequest request);
}
