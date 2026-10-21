package com.unify.onyak.domain.users.profile.service;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.profile.dto.response.MyProfileResponse;

public interface MyProfileService {
    MyProfileResponse getMyProfile(Long userId, Role role);
}
