package com.unify.onyak.domain.wards.auth.dto.request;

import com.unify.onyak.domain.users.auth.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WardSignUpRequest(

        @NotBlank
        String name,

        @NotNull
        LocalDate birthDate,

        @NotNull
        Float weight,

        @NotNull
        Float height,

        @NotNull
        Gender gender,

        @NotNull
        Boolean isPregnant,

        @NotNull
        Boolean medicationPushEnabled,

        @NotNull
        Boolean sosPushEnabled
){
}
