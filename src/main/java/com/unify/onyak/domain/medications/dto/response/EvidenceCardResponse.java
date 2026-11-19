package com.unify.onyak.domain.medications.dto.response;

import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Evidence;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Finding;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 6.2 복약 확인 근거 카드. 피보호자 약장(복용 중인 약 전체)을 DUR 로 점검한 결과.
 * level: DANGER(빨강) / CAUTION(노랑) / OK / EMPTY(복용 중인 약 없음)
 */
public record EvidenceCardResponse(
        Long wardId,
        String wardName,
        OffsetDateTime generatedAt,
        Patient patient,
        String level,
        String headline,
        List<CardMedication> medications,
        List<CardInteraction> interactions,
        List<String> warnings,
        String disclaimer
) {
    public static final String LEVEL_EMPTY = "EMPTY";

    /** DUR 점검에 쓴 환자 정보 — 나이에 따라 노인주의·연령금기, 임신 여부에 따라 임부금기가 달라진다. */
    public record Patient(Integer age, Boolean pregnant) {
    }

    /** 약 하나: 전문/일반 판정 근거 + 이 환자에게 해당하는 DUR 항목. */
    public record CardMedication(
            Long medicationId,
            String drugId,
            String itemName,
            String classification,
            Double confidence,
            List<Evidence> evidences,
            List<Finding> findings,
            Map<String, Integer> summary,
            boolean lookupFailed,
            boolean hasWarning
    ) {
    }

    /** 약장 안 두 약 사이의 문제. kind: 병용금기 / 성분중복 */
    public record CardInteraction(
            String kind,
            String severity,
            List<Long> medicationIds,
            List<String> itemNames,
            String detail
    ) {
    }
}
