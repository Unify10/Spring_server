package com.unify.onyak.domain.wards.profile.service;

import com.unify.onyak.global.security.CustomUserDetails;
import com.unify.onyak.domain.wards.profile.dto.request.WardProfileEditRequest;
import com.unify.onyak.domain.wards.profile.dto.response.WardProfileEditResponse;

public interface WardProfileEditService {
    WardProfileEditResponse editWardProfile(long wardId, CustomUserDetails userDetails, WardProfileEditRequest request);
}
