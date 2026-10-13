package com.unify.onyak.domain.wards.auth.service.impl;

import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.UserIdNotFoundException;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.policy.WardValidationPolicy;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.auth.service.WardSignUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class WardSignUpServiceImpl implements WardSignUpService {

    private final UserRepository userRepository;
    private final WardRepository wardRepository;
    private final WardValidationPolicy wardValidationPolicy;

    @Transactional
    @Override
    public WardSignUpResponse signUp(WardSignUpRequest request){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long requesterId = Long.valueOf(authentication.getName());

        wardValidationPolicy.validateGuardianId(request.guardianId(),requesterId);

        User guardian = userRepository.findById(request.guardianId())
                .orElseThrow(()-> new UserIdNotFoundException());

        Ward ward = Ward.createLocal(
                guardian,
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
