package com.unify.onyak.domain.medications.dto.response;

import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Evidence;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Finding;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.CardInteraction;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.Patient;

import java.util.List;
import java.util.Map;

/**
 * 4.1 병용금기 · 4.2 중복 성분 · 4.3 연령/임부 금기 — 약장에 담기 "전에" 보여주는 점검 결과.
 * <ul>
 *   <li>level: DANGER(빨강) / CAUTION(노랑) / OK / UNKNOWN(약 정보를 못 불러와 점검 불가)</li>
 *   <li>interactions: 새 약과 지금 복용 중인 약 사이의 문제만. medication_ids 는 걸린 기존 약</li>
 *   <li>drug: 새 약 자체의 전문/일반 판정 근거와 이 환자에게 해당하는 DUR 항목</li>
 * </ul>
 * 약장에 저장하지 않으며 analysis_results 도 남기지 않는다.
 */
public record MedicationPrecheckResponse(
        Long wardId,
        Patient patient,
        String level,
        String headline,
        boolean alreadyTaking,
        PrecheckDrug drug,
        List<CardInteraction> interactions,
        List<String> warnings,
        String disclaimer
) {
    public static final String LEVEL_DANGER = "DANGER";
    public static final String LEVEL_CAUTION = "CAUTION";
    public static final String LEVEL_OK = "OK";
    public static final String LEVEL_UNKNOWN = "UNKNOWN";

    public record PrecheckDrug(
            String drugId,
            String itemName,
            String classification,
            Double confidence,
            List<Evidence> evidences,
            List<Finding> findings,
            Map<String, Integer> summary,
            boolean lookupFailed
    ) {
    }
}
