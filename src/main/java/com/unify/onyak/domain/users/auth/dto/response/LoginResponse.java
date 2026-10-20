package com.unify.onyak.domain.users.auth.dto.response;

public record LoginResponse(
        String accessToken,

        String refreshToken
) {
}
