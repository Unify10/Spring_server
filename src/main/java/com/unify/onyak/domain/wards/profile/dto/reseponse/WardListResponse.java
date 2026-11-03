package com.unify.onyak.domain.wards.profile.dto.reseponse;

import com.unify.onyak.domain.wards.auth.entity.Ward;

public record WardListResponse(
        Long id,
        String name
) {
    public static WardListResponse from(Ward ward){
        return new WardListResponse(ward.getId(), ward.getName());
    }
}
