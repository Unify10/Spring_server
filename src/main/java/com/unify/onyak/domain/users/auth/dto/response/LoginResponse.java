package com.unify.onyak.domain.users.auth.dto.response;

import com.unify.onyak.domain.users.auth.entity.Role;

public record LoginResponse(
        Long userId,

        String email,

        String name,

        Role role,

        String accessToken,

        String refreshToken
) {
}
