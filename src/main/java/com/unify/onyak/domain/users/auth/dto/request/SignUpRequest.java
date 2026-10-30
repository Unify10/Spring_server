package com.unify.onyak.domain.users.auth.dto.request;

import com.unify.onyak.domain.users.auth.entity.Gender;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record SignUpRequest(
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

        LocalDate birthDate,

        Float weight,

        Float height,

        Gender gender,

        Boolean isPregnant,

        Boolean medicationPushEnabled,

        Boolean sosPushEnabled,

        Boolean marketingPushEnabled
) {
}
