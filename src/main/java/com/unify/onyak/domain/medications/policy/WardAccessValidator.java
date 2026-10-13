package com.unify.onyak.domain.medications.policy;

import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.error.exception.WardNotFoundException;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 약장 API 공통 권한 검사.
 * <ul>
 *   <li>GUARDIAN: 자기가 등록한 피보호자만 (ward.guardian.id == userId)</li>
 *   <li>WARD: 자기 자신만 (토큰의 userId 가 곧 wardId — SOS 와 같은 규칙)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class WardAccessValidator {

    private static final String GUARDIAN = "GUARDIAN";
    private static final String WARD = "WARD";

    private final WardRepository wardRepository;

    public Ward validate(CustomUserDetails user, Long wardId) {
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(WardNotFoundException::new);
        check(user, ward);
        return ward;
    }

    /** 이미 불러온 ward 로 검사한다 (/medications/{medId} 계열). */
    public void check(CustomUserDetails user, Ward ward) {
        if (user == null || !canAccess(user, ward)) {
            throw new WardAccessDeniedException();
        }
    }

    private boolean canAccess(CustomUserDetails user, Ward ward) {
        Long userId = user.getUserId();
        return switch (user.getRoleName()) {
            case GUARDIAN -> ward.getGuardianId() != null
                    && userId.equals(ward.getGuardianId().getId());
            case WARD -> userId.equals(ward.getId());
            default -> false;
        };
    }
}
