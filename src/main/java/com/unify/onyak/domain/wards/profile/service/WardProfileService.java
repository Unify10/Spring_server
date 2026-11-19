package com.unify.onyak.domain.wards.profile.service;

import com.unify.onyak.domain.wards.profile.dto.response.WardDetailResponse;
import com.unify.onyak.domain.wards.profile.dto.response.WardListResponse;
import com.unify.onyak.global.security.CustomUserDetails;

import java.util.List;

public interface WardProfileService{

    List<WardListResponse> getWards(CustomUserDetails userDetails);

    WardDetailResponse getWard(CustomUserDetails userDetails, Long wardId);
}
