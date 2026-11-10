package com.unify.onyak.domain.wards.profile.service.impl;

import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.users.auth.entity.Gender;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.profile.error.exception.InvalidPregnancy;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.error.exception.GuardianRoleRequiredException;
import com.unify.onyak.domain.wards.profile.dto.request.WardProfileEditRequest;
import com.unify.onyak.domain.wards.profile.dto.response.WardProfileEditResponse;
import com.unify.onyak.domain.wards.profile.service.WardProfileEditService;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@RequiredArgsConstructor
@Service
public class WardProfileEditServiceImpl implements WardProfileEditService {

    private final WardAccessValidator wardAccessValidator;

    @Transactional
    @Override
    public WardProfileEditResponse editWardProfile(long wardId, CustomUserDetails userDetails, WardProfileEditRequest request) {
        if (!Role.GUARDIAN.name().equals(userDetails.getRoleName())) {
            throw new GuardianRoleRequiredException();
        }

        Ward ward = wardAccessValidator.validate(userDetails, wardId, Access.GUARDIAN_ONLY);
        Gender finalGender = request.gender() != null ? request.gender() : ward.getGender();
        if (finalGender == Gender.M && Boolean.TRUE.equals(request.isPregnant())) throw new InvalidPregnancy();

        ward.updateInformation(
                request.name(),
                request.weight(),
                request.height(),
                request.gender(),
                request.isPregnant()
        );

        return WardProfileEditResponse.builder()
                .wardId(ward.getId())
                .name(ward.getName())
                .weight(ward.getWeight())
                .height(ward.getHeight())
                .gender(ward.getGender().name())
                .isPregnant(Boolean.TRUE.equals(ward.getIsPregnant()))
                .build();
    }
}
