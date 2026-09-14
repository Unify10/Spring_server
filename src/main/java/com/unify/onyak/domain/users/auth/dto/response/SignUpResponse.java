package com.unify.onyak.domain.users.auth.dto.response;

public record SignUpResponse(
        Long userId,

        String email,

        String name

        //  jwt 로직 완성 후 accessToken, RefreshToken 전달 예정
) {
}
