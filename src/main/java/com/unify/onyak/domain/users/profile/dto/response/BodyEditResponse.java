package com.unify.onyak.domain.users.profile.dto.response;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.User;

import java.time.LocalDate;

public record BodyEditResponse(
        LocalDate birthDate,
        Float weight,
        Float height,
        Gender gender,
        Boolean isPregnant
) {
    public static BodyEditResponse from(User user) {
        return new BodyEditResponse(
                user.getBirthDate(),
                user.getWeight(),
                user.getHeight(),
                user.getGender(),
                user.getIsPregnant()
        );
    }
}