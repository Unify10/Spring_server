package com.unify.onyak.domain.wards.profile.dto.request;

import com.unify.onyak.domain.users.auth.entity.Gender;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record WardProfileEditRequest(
        @Size(min = 1, max = 100)
        String name,

        @Positive
        Float weight,

        @Positive
        Float height,

        Gender gender,

        Boolean isPregnant
) {
}
