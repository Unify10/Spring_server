package com.unify.onyak.domain.users.profile.service;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.profile.dto.request.BodyEditRequest;
import com.unify.onyak.domain.users.profile.dto.response.BodyEditResponse;

public interface BodyEditService {
    BodyEditResponse editBody(Long userId, Role role, BodyEditRequest request);
}