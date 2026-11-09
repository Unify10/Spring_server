package com.unify.onyak.domain.wards.profile.service.impl;

import com.unify.onyak.domain.medications.error.exception.WardNotFoundException;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.domain.wards.profile.dto.request.WardProfileEditRequest;
import com.unify.onyak.domain.wards.profile.dto.response.WardProfileEditResponse;
import com.unify.onyak.domain.wards.profile.error.exception.WardUnauthorized;
import com.unify.onyak.domain.wards.profile.service.WardProfileEditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class WardProfileEditServiceImpl implements WardProfileEditService {

    private final WardRepository wardRepository;

    @Transactional
    @Override
    public WardProfileEditResponse editWardProfile(long wardId, long guardianId, Role role, WardProfileEditRequest request) {

        if(role != Role.GUARDIAN) throw new WardUnauthorized();

        List<Ward> wards = wardRepository.findByGuardianId_Id(guardianId);
        Ward ward = null;

        for(Ward item : wards)
        {
            if(item.getId().equals(wardId))
                ward = item;
        }

        if(ward == null) throw new WardNotFoundException();
        // 피보호자를 피보호자 id로 찾는 메서드가 없어 보호자로 먼저 찾고 피보호자를 찾아 본인이 관리하지 않는 피보호자의 id를 줬을 때에도 404가 나옴.

        ward.updateInformation(
                request.name(),
                request.weight(),
                request.height(),
                request.gender(),
                request.isPregnant()
        );

        return WardProfileEditResponse.builder()
                .wardId(wardId)
                .weight(request.weight())
                .height(request.height())
                .gender(request.gender())
                .isPregnant(request.isPregnant())
                .build();
    }
}
