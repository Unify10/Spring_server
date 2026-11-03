package com.unify.onyak.domain.wards.profile.dto.reseponse;

import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.wards.auth.entity.Ward;

import java.time.LocalDate;

public record WardDetailResponse(
        Long wardId,
        String name,
        LocalDate birthDate,
        Float weight,
        Float height,
        Gender gender,
        Boolean isPregnant,
        Boolean medicationPushEnabled,
        Boolean sosPushEnabled
) {
    public static WardDetailResponse from(Ward ward){
        return new WardDetailResponse(
                ward.getId(),
                ward.getName(),
                ward.getBirthDate(),
                ward.getWeight(),
                ward.getHeight(),
                ward.getGender(),
                ward.getIsPregnant(),
                ward.getMedicationPushEnabled(),
                ward.getSosPushEnabled()
        );
    }
}
