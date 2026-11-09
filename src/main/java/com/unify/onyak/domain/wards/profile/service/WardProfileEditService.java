package com.unify.onyak.domain.wards.profile.service;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.wards.profile.dto.request.WardProfileEditRequest;
import com.unify.onyak.domain.wards.profile.dto.response.WardProfileEditResponse;

public interface WardProfileEditService {
    WardProfileEditResponse editWardProfile(long wardId, long userId, Role role, WardProfileEditRequest request);
}
