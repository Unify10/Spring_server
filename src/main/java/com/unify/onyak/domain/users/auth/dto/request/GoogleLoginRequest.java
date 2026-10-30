package com.unify.onyak.domain.users.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unify.onyak.domain.users.auth.service.impl.GoogleIdTokenVerifierImpl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank(message = "구글 ID 토큰은 필수입니다.")
        @Size(max = GoogleIdTokenVerifierImpl.MAX_TOKEN_LENGTH, message = "구글 ID 토큰 길이가 너무 깁니다.")
        @JsonProperty("id_token")
        String idToken
) {
}
