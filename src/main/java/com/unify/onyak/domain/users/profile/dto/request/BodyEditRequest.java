package com.unify.onyak.domain.users.profile.dto.request;

import com.unify.onyak.domain.users.auth.entity.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record BodyEditRequest(
        @Past
        LocalDate birthDate,

        @Positive @DecimalMax("300.0")
        Float weight,

        @Positive @DecimalMax("300.0")
        Float height,

        Gender gender,

        Boolean isPregnant
) {
}