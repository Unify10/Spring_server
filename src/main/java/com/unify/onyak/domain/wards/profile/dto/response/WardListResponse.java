package com.unify.onyak.domain.wards.profile.dto.response;

import com.unify.onyak.domain.wards.auth.entity.Ward;

public record WardListResponse(
        Long wardId,
        String name
) {
    public static WardListResponse from(Ward ward){
        return new WardListResponse(ward.getId(), ward.getName());
    }
}
