package com.unify.onyak.domain.wards.auth.dto.request;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

public record WardSignUpRequest(

        @NotBlank
        String name,

        @NotNull
        Date birthDate,

        @NotNull
        Float weight,

        @NotNull
        Float height,

        @NotNull
        Gender gender,

        @NotNull
        boolean isPregnant,

        @NotNull
        boolean medicationPushEnabled,

        @NotNull
        boolean sosPushEnabled
){
}
