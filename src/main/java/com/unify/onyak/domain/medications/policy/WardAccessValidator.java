package com.unify.onyak.domain.medications.policy;

import com.unify.onyak.domain.medications.error.exception.MedicationWriteForbiddenException;
import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.error.exception.WardNotFoundException;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 약장 API 공통 권한 검사 (기능명세 5.x / API 명세 기준).
 * <table>
 *   <tr><th></th><th>WARD_OR_GUARDIAN<br>조회·복용 일정 변경·근거 카드</th><th>GUARDIAN_ONLY<br>담기·중단·재개·용량 변경·삭제</th></tr>
 *   <tr><td>보호자</td><td>자기 피보호자</td><td>자기 피보호자</td></tr>
 *   <tr><td>피보호자</td><td>본인</td><td>불가 (403)</td></tr>
 * </table>
 * 피보호자 토큰의 userId 는 곧 wardId 다 (SOS 와 같은 규칙).
 */
@Component
@RequiredArgsConstructor
public class WardAccessValidator {

    public enum Access {
        /** 보호자(자기 피보호자) + 피보호자 본인 */
        WARD_OR_GUARDIAN,
        /** 보호자(자기 피보호자)만 */
        GUARDIAN_ONLY
    }

    private static final String GUARDIAN = "GUARDIAN";
    private static final String WARD = "WARD";

    private final WardRepository wardRepository;

    public Ward validate(CustomUserDetails user, Long wardId, Access access) {
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(WardNotFoundException::new);
        check(user, ward, access);
        return ward;
    }

    /** 이미 불러온 ward 로 검사한다 (/medications/{medId} 계열). */
    public void check(CustomUserDetails user, Ward ward, Access access) {
        if (user == null) {
            throw new WardAccessDeniedException();
        }
        switch (user.getRoleName()) {
            case GUARDIAN -> {
                if (ward.getGuardianId() == null || !user.getUserId().equals(ward.getGuardianId().getId())) {
                    throw new WardAccessDeniedException();
                }
            }
            case WARD -> {
                if (!user.getUserId().equals(ward.getId())) {
                    throw new WardAccessDeniedException();
                }
                if (access == Access.GUARDIAN_ONLY) {
                    throw new MedicationWriteForbiddenException();
                }
            }
            default -> throw new WardAccessDeniedException();
        }
    }
}
