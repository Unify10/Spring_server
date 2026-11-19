package com.unify.onyak.domain.medications.service;

import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse;
import com.unify.onyak.global.security.CustomUserDetails;

public interface EvidenceCardService {

    /** 6.2 피보호자 약장 전체의 DUR 근거 카드를 만들고, 약별 스냅샷을 analysis_results 에 남긴다. */
    EvidenceCardResponse getEvidenceCard(CustomUserDetails user, Long wardId);
}
