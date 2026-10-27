package com.unify.onyak.domain.users.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank(message = "구글 ID 토큰은 필수입니다.")
        @Size(max = 4096, message = "구글 ID 토큰 길이가 너무 깁니다.")
        @JsonProperty("id_token")
        String idToken
) {
}
