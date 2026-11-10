package com.unify.onyak.domain.medications.controller;

import com.unify.onyak.domain.medications.dto.request.MedicationPrecheckRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationPrecheckResponse;
import com.unify.onyak.domain.medications.service.MedicationPrecheckService;
import com.unify.onyak.global.response.ApiResponse;
import com.unify.onyak.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 4.1 병용금기 · 4.2 중복 성분 · 4.3 연령/임부 금기 — 약장에 담기 전 미리 점검. */
@RestController
@RequiredArgsConstructor
public class MedicationPrecheckController {

    private final MedicationPrecheckService medicationPrecheckService;

    // 보호자(자기 피보호자)·피보호자 본인 모두. 저장은 하지 않는다.
    @PostMapping("/wards/{wardId}/medications/precheck")
    public ApiResponse<MedicationPrecheckResponse> precheck(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long wardId,
            @RequestBody @Valid MedicationPrecheckRequest request
    ) {
        return ApiResponse.ok(medicationPrecheckService.precheck(userDetails, wardId, request));
    }
}
