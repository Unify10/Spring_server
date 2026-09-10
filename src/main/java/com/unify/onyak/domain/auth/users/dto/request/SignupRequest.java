package com.unify.onyak.domain.auth.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        @NotBlank
        String passwordConfirm,

        @NotBlank
        String name,

        @NotBlank
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String phone,

        String age,

        String weight,

        String gender,

        boolean isPregnant,

        boolean medicationPushEnabled,

        boolean sosPushEnabled
) {
}
