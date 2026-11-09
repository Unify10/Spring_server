package com.unify.onyak.domain.wards.profile.dto.request;

public record WardProfileEditRequest(
        String name,

        Float weight,

        Float height,

        String gender,

        Boolean isPregnant
) {
}
