package com.unify.onyak.domain.users.auth.dto.response;

public record SignUpResponse(
        Long userId,

        String email,

        String name,

        String accessToken,

        String refreshToken
) {
}
