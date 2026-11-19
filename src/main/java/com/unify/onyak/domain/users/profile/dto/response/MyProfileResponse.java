package com.unify.onyak.domain.users.profile.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record MyProfileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        long userId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean emailVerified,

        Integer age,

        Float weight,

        String gender,

        Boolean isPregnant
) {
}
