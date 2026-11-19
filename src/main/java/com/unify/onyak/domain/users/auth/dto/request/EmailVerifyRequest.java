package com.unify.onyak.domain.users.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailVerifyRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Pattern(regexp = "^[0-9A-Z]{6}$", message = "인증코드는 숫자와 대문자로 이루어진 6자리입니다.")
        String code
) {
}
