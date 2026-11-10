package com.unify.onyak.domain.medications.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /wards/{wardId}/medications/precheck — 약장에 담기 전에 미리 점검할 약. */
public record MedicationPrecheckRequest(
        // /drugs/search·/drugs/barcode·/drugs/analyze 가 돌려준 품목기준코드 (drug_id)
        @NotBlank
        @Size(max = 20)
        String itemSeq
) {
}
