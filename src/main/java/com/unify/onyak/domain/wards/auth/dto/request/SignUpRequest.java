package com.unify.onyak.domain.wards.auth.dto.request;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;

public record SignUpRequest(@NotBlank

        @NotBlank
        User guardianId,

        @NotBlank
        String name,

        @NotBlank
        Date birthDate,

        @NotBlank
        Float weight,

        @NotBlank
        Float height,

        @NotBlank
        Gender gender,

        @NotBlank
        boolean isPregnant,

        @NotBlank
        boolean medicationPushEnabled,

        @NotBlank
        boolean sosPushEnabled
){
}
