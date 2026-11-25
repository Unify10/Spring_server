package com.unify.onyak.domain.wards.link_codes.service.impl;

import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.users.auth.entity.Role;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.error.exception.GuardianRoleRequiredException;
import com.unify.onyak.domain.wards.link_codes.entity.LinkCodeStatus;
import com.unify.onyak.domain.wards.link_codes.repository.LinkCodeRepository;
import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;
import com.unify.onyak.domain.wards.link_codes.service.LinkCodeReissueService;
import com.unify.onyak.domain.wards.link_codes.service.LinkCodeService;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LinkCodeReissueServiceImpl implements LinkCodeReissueService {

    private final WardAccessValidator wardAccessValidator;
    private final LinkCodeRepository linkCodeRepository;
    private final LinkCodeService linkCodeService;

    // 보호자가 자기 피보호자의 연동 코드를 다시 받는다.
    // 이미 로그인한 피보호자 기기(refresh 토큰)는 그대로 두고, 새 기기에서 로그인할 코드만 발급한다.
    @Override
    @Transactional
    public LinkCodeResponse reissue(CustomUserDetails userDetails, Long wardId) {
        if (!Role.GUARDIAN.name().equals(userDetails.getRoleName())) {
            throw new GuardianRoleRequiredException();
        }

        // 없는 피보호자면 404, 다른 보호자의 피보호자면 403
        Ward ward = wardAccessValidator.validate(userDetails, wardId, Access.GUARDIAN_ONLY);

        // 유효한 코드는 항상 하나만 남도록 이전 코드를 먼저 만료시킨다
        linkCodeRepository.expirePendingCodesOfWard(ward.getId(), LinkCodeStatus.PENDING, LinkCodeStatus.EXPIRED);

        return linkCodeService.createLinkCode(ward);
    }
}
