package com.unify.onyak.domain.medications.service;

import com.unify.onyak.domain.medications.dto.request.MedicationPrecheckRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationPrecheckResponse;
import com.unify.onyak.global.security.CustomUserDetails;

public interface MedicationPrecheckService {

    /** 4.1~4.3 새 약을 약장에 담기 전에, 지금 복용 중인 약·환자 정보와 부딪히는지 점검한다. */
    MedicationPrecheckResponse precheck(CustomUserDetails user, Long wardId, MedicationPrecheckRequest request);
}
