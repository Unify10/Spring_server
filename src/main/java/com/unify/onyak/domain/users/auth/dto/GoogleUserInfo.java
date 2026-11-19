package com.unify.onyak.domain.users.auth.dto;

public record GoogleUserInfo(
        String googleId,

        String email,

        String name
) {
}
