package com.unify.onyak.domain.wards.profile.service.impl;

import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.error.exception.GuardianRoleRequiredException;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
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

    private final WardAccessValidator wardAccessValidator;

    @Override
    public List<WardListResponse> getWards(CustomUserDetails userDetails) {
        validateGuardian(userDetails);

        return wardRepository.findByGuardianId_Id(userDetails.getUserId())
                .stream()
                .map(WardListResponse::from)
                .toList();
    }

    @Override
    public WardDetailResponse getWard(CustomUserDetails userDetails, Long wardId) {
        // 보호자는 자기 피보호자, 피보호자는 본인(토큰 userId == wardId)만 조회할 수 있다
        Ward ward = wardAccessValidator.validate(userDetails, wardId, Access.WARD_OR_GUARDIAN);

        return WardDetailResponse.from(ward);
    }

    private void validateGuardian(CustomUserDetails userDetails) {
        if (!Role.GUARDIAN.name().equals(userDetails.getRoleName())) {
            throw new GuardianRoleRequiredException();
        }
    }
}
