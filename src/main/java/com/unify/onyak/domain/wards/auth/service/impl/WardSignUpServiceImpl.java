package com.unify.onyak.domain.wards.auth.service.impl;

import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.policy.WardValidationPolicy;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.auth.service.WardSignUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class WardSignUpServiceImpl implements WardSignUpService {

    private final WardRepository wardRepository;
    private final WardValidationPolicy wardValidationPolicy;

    @Transactional
    @Override
    public WardSignUpResponse signUp(WardSignUpRequest request){

        wardValidationPolicy.ValidateGuardianId(request.guardianId());

        Ward ward = Ward.createLocal(
                request.guardianId(),
                request.name(),
                request.birthDate(),
                request.weight(),
                request.height(),
                request.gender(),
                request.isPregnant(),
                request.medicationPushEnabled(),
                request.sosPushEnabled()
        );

        wardRepository.save(ward);

        return new WardSignUpResponse(ward.getId(),ward.getName());
    }

}
