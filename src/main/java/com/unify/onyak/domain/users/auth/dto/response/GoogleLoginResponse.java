package com.unify.onyak.domain.users.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unify.onyak.domain.users.auth.entity.Role;

public record GoogleLoginResponse(
        Long userId,

        String email,

        String name,

        Role role,

        String accessToken,

        String refreshToken,

        @JsonProperty("is_new_user")
        Boolean isNewUser
) {
}
