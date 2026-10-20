package com.unify.onyak.domain.wards.auth.service.impl;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.users.auth.error.exception.UserIdNotFoundException;
import com.unify.onyak.domain.users.auth.repository.UserRepository;
import com.unify.onyak.domain.wards.auth.dto.request.WardSignUpRequest;
import com.unify.onyak.domain.wards.auth.dto.response.WardSignUpResponse;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.error.exception.GuardianRoleRequiredException;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.auth.service.WardSignUpService;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class WardSignUpServiceImpl implements WardSignUpService {

    private final UserRepository userRepository;
    private final WardRepository wardRepository;

    @Override
    @Transactional
    public WardSignUpResponse signUp(CustomUserDetails userDetails, WardSignUpRequest request){

        if (!Role.GUARDIAN.name().equals(userDetails.getRoleName())) {
            throw new GuardianRoleRequiredException();
        }

        User guardian = userRepository.findById(userDetails.getUserId())
                .orElseThrow(UserIdNotFoundException::new);

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
