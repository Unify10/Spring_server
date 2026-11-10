package com.unify.onyak.domain.wards.profile.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record WardProfileEditResponse(
        long wardId,

        @NotBlank
        String name,

        float weight,

        float height,

        @NotBlank
        String gender,

        boolean isPregnant
) {
}
