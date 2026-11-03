package com.unify.onyak.domain.wards.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.error.exception.GuardianRoleRequiredException;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.wards.error.exception.WardNotFoundException;
import com.unify.onyak.domain.wards.profile.dto.response.WardDetailResponse;
import com.unify.onyak.domain.wards.profile.dto.response.WardListResponse;
import com.unify.onyak.domain.wards.profile.service.WardProfileService;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WardProfileServiceImpl implements WardProfileService {

    private final WardRepository wardRepository;

    @Override
    public List<WardListResponse> getWards(CustomUserDetails userDetails){
        validationGuardian(userDetails);

        return wardRepository.findByGuardianId_Id(userDetails.getUserId())
                .stream()
                .map(WardListResponse::from)
                .toList();
        }

    @Override
    public WardDetailResponse getWard(CustomUserDetails userDetails, Long wardId){
        validationGuardian(userDetails);

        Ward ward = wardRepository.findById(wardId).orElseThrow(WardNotFoundException::new);

        if(!ward.getGuardianId().getId().equals(userDetails.getUserId())){
            throw new WardAccessDeniedException();
        }

        return WardDetailResponse.from(ward);
    }

    private void validationGuardian(CustomUserDetails userDetails){
        if(!Role.GUARDIAN.name().equals(userDetails.getRoleName())){
            throw new GuardianRoleRequiredException();
        }
    }
}
