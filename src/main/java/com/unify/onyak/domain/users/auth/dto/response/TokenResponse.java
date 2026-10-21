package com.unify.onyak.domain.users.auth.dto.response;

public record TokenResponse(
        String accessToken,

        String refreshToken
) {
}
